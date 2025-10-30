# PraxCore (Velocity + Paper)

PraxCore es un SDK/plug-in doble para redes de Minecraft:
- **praxcore-velocity** (Proxy Velocity): puente con el backend (SaaS) y emisor de mensajes/claims al juego.
- **praxcore-paper** (Paper/Spigot): lógica de gameplay, GUI, comandos `/register` y `/login`, consumo de datos del proxy.

## 🚀 ¿Qué resuelve?
- **Identidad/sesión unificada** entre proxy y servidores.
- **UX consistente** (menus, permisos, cosméticos).
- **Sincronización de progreso** (stats y eventos de juego).
- **Desacoplamiento** del backend: Paper no llama al backend; habla con el proxy por **Plugin Messaging Channel**.

---

## 📦 Módulos

### praxcore-velocity (Proxy)
- `com.prax.core.velocity.PraxProxyPlugin` → punto de entrada (Velocity).
- `TokenManager` → emisión/validación de tokens, cache de perfiles.
- `resources/velocity-plugin.json`
  ```json
  {
    "id": "praxcore-velocity",
    "name": "PraxCore Velocity",
    "version": "1.0.0",
    "authors": ["akhan"],
    "dependencies": [],
    "main": "com.prax.core.velocity.PraxProxyPlugin"
  }
