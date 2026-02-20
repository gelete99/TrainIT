import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as admin from "firebase-admin";
import OpenAI from "openai";

admin.initializeApp();

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

// Control de coste
const MAX_PLANS_PER_DAY = 10;
const MAX_WORKOUTS = 10;
const MODEL = "gpt-4.1-mini";

type UserProfile = {
  uid: string;
  level: string;
  goal: string;
  daysPerWeek: number;
  heightCm: number;
  weightKg: number;
  age: number;
};

type Workout = {
  id: string;
  date: number;
  type: string;
  durationMin: number;
  rpe: number;
  notes: string;
};

function startOfTodayUtcMillis(): number {
  const now = new Date();
  return Date.UTC(
    now.getUTCFullYear(),
    now.getUTCMonth(),
    now.getUTCDate(),
    0,
    0,
    0,
    0
  );
}

const DAYS_ES = [
  "Lunes",
  "Martes",
  "Miércoles",
  "Jueves",
  "Viernes",
  "Sábado",
  "Domingo",
];

function clampInt(n: any, min: number, max: number, fallback: number): number {
  const x = Number(n);
  if (!Number.isFinite(x)) return fallback;
  return Math.min(max, Math.max(min, Math.trunc(x)));
}

export const generatePlan = onCall(
  { region: "europe-west1", secrets: [OPENAI_API_KEY] },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Debes iniciar sesión.");
    }

    const db = admin.firestore();

    // Rate limit diario
    const todayStart = startOfTodayUtcMillis();
    const usageRef = db.collection("ai_usage").doc(uid);

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(usageRef);
      const data = snap.exists ? (snap.data() as any) : null;

      const lastReset = data?.lastReset ?? 0;
      const count = data?.count ?? 0;

      if (lastReset < todayStart) {
        tx.set(usageRef, { lastReset: todayStart, count: 1 }, { merge: true });
        return;
      }

      if (count >= MAX_PLANS_PER_DAY) {
        throw new HttpsError(
          "resource-exhausted",
          "Límite diario alcanzado. Inténtalo mañana."
        );
      }

      tx.set(usageRef, { count: count + 1 }, { merge: true });
    });

    // Leer perfil
    const profileSnap = await db.collection("users").doc(uid).get();
    if (!profileSnap.exists) {
      throw new HttpsError(
        "failed-precondition",
        "Perfil no encontrado. Completa el onboarding."
      );
    }

    const raw = profileSnap.data() as any;

    // Normaliza por si faltan campos
    const profile: UserProfile = {
      uid,
      level: String(raw.level ?? "principiante"),
      goal: String(raw.goal ?? ""),
      daysPerWeek: clampInt(raw.daysPerWeek, 1, 7, 3),
      heightCm: clampInt(raw.heightCm, 0, 300, 0),
      weightKg: clampInt(raw.weightKg, 0, 500, 0),
      age: clampInt(raw.age, 0, 120, 0),
    };

    // Leer últimos entrenos
    const workoutsSnap = await db
      .collection("users")
      .doc(uid)
      .collection("workouts")
      .orderBy("date", "desc")
      .limit(MAX_WORKOUTS)
      .get();

    const workouts: Workout[] = workoutsSnap.docs.map((d) => {
      const w = d.data() as any;
      return {
        id: String(w.id ?? d.id),
        date: Number(w.date ?? 0),
        type: String(w.type ?? ""),
        durationMin: clampInt(w.durationMin, 0, 300, 0),
        rpe: clampInt(w.rpe, 0, 10, 0),
        notes: String(w.notes ?? ""),
      };
    });

    // Prompt (JSON estructurado pro)
    const system = `
Eres un entrenador personal. Devuelve SIEMPRE un JSON válido (sin markdown, sin texto extra).
Todo en español y conciso.

Estructura EXACTA:
{
  "generatedAt": 0,
  "assessment": {
    "summary": "",
    "strengths": [""],
    "improvements": [""]
  },
  "recommendations": [""],
  "weeklyPlan": [
    {
      "day": "Lunes",
      "isTrainingDay": true,
      "focus": "",
      "durationMin": 45,
      "targetRpe": 7,
      "session": [
        {"name":"", "sets":3, "reps":"8-10"}
      ],
      "notes": ""
    }
  ],
  "safetyNotes": [""]
}

Reglas IMPORTANTES:
- weeklyPlan DEBE tener SIEMPRE 7 elementos (Lunes a Domingo, en ese orden exacto).
- El número de días con isTrainingDay=true debe ser EXACTAMENTE daysPerWeek.
- En días de descanso: isTrainingDay=false, focus="Descanso/Movilidad", durationMin=0, targetRpe=0, session=[], notes breve.
- En días de entrenamiento: durationMin 30-60.
- targetRpe 5-8 según nivel y carga reciente (usa escala RPE 0–10).
- Adapta el plan al objetivo y al historial reciente. Si el historial es escaso, crea un plan seguro y progresivo.
- No des consejos médicos ni diagnósticos.
`.trim();

    const openai = new OpenAI({ apiKey: OPENAI_API_KEY.value() });

    const payload = {
      profile,
      workouts,
      constraints: {
        daysEs: DAYS_ES,
        maxWorkoutsProvided: MAX_WORKOUTS,
      },
    };

    // Llamada OpenAI con manejo de errores (cuota, etc.)
    let content = "";

    try {
      const completion = await openai.chat.completions.create({
        model: MODEL,
        temperature: 0.3,
        messages: [
          { role: "system", content: system },
          { role: "user", content: JSON.stringify(payload) },
        ],
      });

      content = completion.choices[0]?.message?.content ?? "";
    } catch (err: any) {
      const status = err?.status;
      const code = err?.code;
      const msg = err?.error?.message || err?.message || "Error llamando a OpenAI";

      console.error("OpenAI error:", { status, code, msg });

      if (status === 429 || code === "insufficient_quota") {
        throw new HttpsError(
          "resource-exhausted",
          "OpenAI sin cuota/saldo. Revisa la facturación o límites de la API key."
        );
      }

      throw new HttpsError(
        "internal",
        "Error generando el plan. Inténtalo más tarde."
      );
    }

    // Parse JSON
    let json: any;
    try {
      json = JSON.parse(content);
    } catch (e) {
      console.error("JSON parse failed. Raw content:", content);
      throw new HttpsError("internal", "Respuesta IA inválida (JSON).");
    }

    if (!json || typeof json !== "object") {
      throw new HttpsError("internal", "Respuesta IA inválida.");
    }

    json.generatedAt = Date.now();

    if (!Array.isArray(json.weeklyPlan)) {
      console.error("weeklyPlan missing/invalid. Raw json:", json);
      throw new HttpsError("internal", "Plan inválido (weeklyPlan).");
    }

    // Normalizar 7 días y orden Lunes..Domingo
    const planMap = new Map<string, any>();
    for (const item of json.weeklyPlan) {
      if (item && typeof item === "object" && typeof item.day === "string") {
        planMap.set(item.day, item);
      }
    }

    const normalizedWeeklyPlan = DAYS_ES.map((day) => {
      const item = planMap.get(day);
      if (item && typeof item === "object") return item;

      return {
        day,
        isTrainingDay: false,
        focus: "Descanso/Movilidad",
        durationMin: 0,
        targetRpe: 0,
        session: [],
        notes: "Descanso activo suave si te apetece (movilidad 10–15 min).",
      };
    });

    json.weeklyPlan = normalizedWeeklyPlan;

    // Asegurar exactamente daysPerWeek días de entrenamiento
    const desired = clampInt(profile.daysPerWeek, 1, 7, 3);

    let trainingIdxs = json.weeklyPlan
      .map((d: any, i: number) => ({ i, isTrainingDay: !!d?.isTrainingDay }))
      .filter((x: any) => x.isTrainingDay)
      .map((x: any) => x.i);

    if (trainingIdxs.length > desired) {
      const toTurnOff = trainingIdxs.slice(desired);
      for (const i of toTurnOff) {
        json.weeklyPlan[i] = {
          ...json.weeklyPlan[i],
          isTrainingDay: false,
          focus: "Descanso/Movilidad",
          durationMin: 0,
          targetRpe: 0,
          session: [],
          notes: "Descanso para favorecer recuperación y progresión.",
        };
      }
    } else if (trainingIdxs.length < desired) {
      for (
        let i = 0;
        i < json.weeklyPlan.length && trainingIdxs.length < desired;
        i++
      ) {
        if (!json.weeklyPlan[i]?.isTrainingDay) {
          json.weeklyPlan[i] = {
            ...json.weeklyPlan[i],
            isTrainingDay: true,
            focus:
              json.weeklyPlan[i]?.focus &&
              json.weeklyPlan[i]?.focus !== "Descanso/Movilidad"
                ? json.weeklyPlan[i].focus
                : "Sesión de entrenamiento",
            durationMin: clampInt(json.weeklyPlan[i]?.durationMin, 30, 60, 45),
            targetRpe: clampInt(json.weeklyPlan[i]?.targetRpe, 5, 8, 7),
            session: Array.isArray(json.weeklyPlan[i]?.session)
              ? json.weeklyPlan[i].session
              : [],
            notes:
              typeof json.weeklyPlan[i]?.notes === "string"
                ? json.weeklyPlan[i].notes
                : "",
          };
          trainingIdxs.push(i);
        }
      }
    }

    // Asegurar campos principales
    if (!json.assessment || typeof json.assessment !== "object") {
      json.assessment = { summary: "", strengths: [], improvements: [] };
    }
    if (!Array.isArray(json.recommendations)) json.recommendations = [];
    if (!Array.isArray(json.safetyNotes)) json.safetyNotes = [];

    return json;
  }
);