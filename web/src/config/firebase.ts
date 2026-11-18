import { initializeApp, FirebaseApp } from 'firebase/app';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  onAuthStateChanged,
  Auth,
  User
} from 'firebase/auth';
import {
  getFirestore,
  Firestore
} from 'firebase/firestore';

/**
 * Configuration officielle du projet Firebase existant (partagé avec l'application Android Kotlin).
 * Source : /firebase-applet-config.json & /app/google-services.json & /firebase.json
 */
export const FIREBASE_CONFIG = {
  projectId: 'gen-lang-client-0345431815',
  appId: '1:105483567356:web:de698cc230161552ba5779',
  apiKey: 'AIzaSyBmYtHZ7E8fQcsTE-fLqBrd1pTT38XY1pA',
  authDomain: 'gen-lang-client-0345431815.firebaseapp.com',
  storageBucket: 'gen-lang-client-0345431815.firebasestorage.app',
  messagingSenderId: '105483567356'
};

/**
 * Base de données Cloud Firestore Enterprise existante utilisée par l'application Android.
 */
export const FIRESTORE_DATABASE_ID =
  'ai-studio-android-remixges-eaf649c7-b982-4cf3-8da2-da71b4dcafb4';

let appInstance: FirebaseApp | null = null;
let authInstance: Auth | null = null;
let dbInstance: Firestore | null = null;

try {
  appInstance = initializeApp(FIREBASE_CONFIG);
  authInstance = getAuth(appInstance);
  dbInstance = getFirestore(appInstance, FIRESTORE_DATABASE_ID);
} catch (error) {
  console.warn('Initialisation Firebase Web en mode autonome :', error);
}

export const firebaseApp = appInstance;
export const auth = authInstance;
export const db = dbInstance;
export { GoogleAuthProvider, signInWithPopup, onAuthStateChanged };
export type { User };
