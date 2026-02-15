import {onCall, HttpsError} from "firebase-functions/v2/https";
import {defineSecret} from "firebase-functions/params";
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
  hasEquipment: boolean;
};

type Workout = {
  id: string;
  date: number;
  type: string;
  durationMin: number;
  notes: string;
};

function startOfTodayUtcMillis(): number {
  const now = new Date();
  return Date.UTC(
    now.getUTCFullYear(),
    now.getUTCMonth(),
    now.getUTCDate(),
    0, 0, 0, 0
  );
}

export const generatePlan = onCall(
  {region: "europe-west1", secrets: [OPENAI_API_KEY]},
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Debes iniciar sesión.");
    }

    const db = admin.firestore();

    // Rate limit diario (coste controlado)
    const todayStart = startOfTodayUtcMillis();
    const usageRef = db.collection("ai_usage").doc(uid);

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(usageRef);
      const data = snap.exists ? (snap.data() as any) : null;

      const lastReset = data?.lastReset ?? 0;
      const count = data?.count ?? 0;

      if (lastReset < todayStart) {
        tx.set(usageRef, {lastReset: todayStart, count: 1}, {merge: true});
        return;
      }

      if (count >= MAX_PLANS_PER_DAY) {
        throw new HttpsError(
          "resource-exhausted",
          "Límite diario alcanzado. Inténtalo mañana."
        );
      }

      tx.set(usageRef, {count: count + 1}, {merge: true});
    });

    // Leer perfil
    const profileSnap = await db.collection("users").doc(uid).get();
    if (!profileSnap.exists) {
      throw new HttpsError(
        "failed-precondition",
        "Perfil no encontrado. Completa el onboarding."
      );
    }
    const profile = profileSnap.data() as UserProfile;

    // Leer últimos entrenos
    const workoutsSnap = await db
      .collection("users")
      .doc(uid)
      .collection("workouts")
      .orderBy("date", "desc")
      .limit(MAX_WORKOUTS)
      .get();

    const workouts: Workout[] = workoutsSnap.docs.map((d) => d.data() as Workout);

    // Prompt
    const system = `
Eres un entrenador personal. Devuelve SIEMPRE un JSON válido (sin markdown, sin texto extra).
Todo en español y conciso.

Estructura EXACTA:
{
  "meta": {"goal": "...", "level": "...", "daysPerWeek": 3, "hasEquipment": true},
  "plan": [
    {
      "day": "Lunes",
      "title": "Full Body A",
      "durationMin": 45,
      "focus": "Descripción corta",
      "exercises": [
        {"name":"Ejercicio", "sets":3, "reps":"8-10"}
      ]
    }
  ],
  "tips": ["...", "..."]
}

Reglas:
- plan debe tener EXACTAMENTE daysPerWeek elementos.
- Si hasEquipment=false, usa alternativas sin material.
- durationMin 30-60.
`.trim();

    const openai = new OpenAI({apiKey: OPENAI_API_KEY.value()});

    const payload = {profile, recentWorkouts: workouts};

    const completion = await openai.chat.completions.create({
      model: MODEL,
      temperature: 0.3,
      messages: [
        {role: "system", content: system},
        {role: "user", content: JSON.stringify(payload)},
      ],
    });

    const content = completion.choices[0]?.message?.content ?? "";

    let json: any;
    try {
      json = JSON.parse(content);
    } catch {
      throw new HttpsError("internal", "Respuesta IA inválida (JSON).");
    }

    // Validación mínima
    if (!json?.plan || !Array.isArray(json.plan)) {
      throw new HttpsError("internal", "Plan inválido.");
    }

    const desired = Number(profile.daysPerWeek ?? 3);
    if (json.plan.length !== desired) {
      json.plan = json.plan.slice(0, desired);
    }

    json.meta = {
      goal: profile.goal,
      level: profile.level,
      daysPerWeek: desired,
      hasEquipment: profile.hasEquipment,
    };

    return json;
  }
);
