import { useEffect, useState } from "react";

type HealthState = "loading" | "ok" | "error";

function App() {
  const [healthState, setHealthState] = useState<HealthState>("loading");
  const [message, setMessage] = useState("Consultando backend...");

  useEffect(() => {
    const loadHealth = async () => {
      try {
        const response = await fetch("/api/health");

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const data: { status?: string } = await response.json();

        if (data.status === "ok") {
          setHealthState("ok");
          setMessage("Backend conectado correctamente.");
          return;
        }

        throw new Error("Unexpected response");
      } catch {
        setHealthState("error");
        setMessage("No se pudo obtener una respuesta valida del backend.");
      }
    };

    void loadHealth();
  }, []);

  return (
    <main className="app-shell">
      <section className="hero-card">
        <p className="eyebrow">Frontend inicial</p>
        <h1>MineDaily</h1>
        <p className="description">
          React + Vite + TypeScript conectados al backend de Spring Boot.
        </p>

        <div className={`status status-${healthState}`}>
          <span className="status-label">Estado del backend</span>
          <strong>{message}</strong>
        </div>
      </section>
    </main>
  );
}

export default App;
