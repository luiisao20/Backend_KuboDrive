# Guía de Configuración del Servidor Ubuntu (KuboDrive)

Este documento detalla los pasos realizados para configurar desde cero el servidor local (Sony Vaio) que hospeda la base de datos y el almacenamiento de archivos de KuboDrive.

## 1. Recuperación de Acceso (Recovery Mode)
Si se pierde la contraseña del servidor:
1. Reiniciar y mantener presionado `Shift` o `Esc` para entrar al menú **GRUB**.
2. Ir a **Advanced options for Ubuntu** > **(recovery mode)**.
3. Elegir `root - Drop to root shell prompt`.
4. Montar el sistema de archivos con escritura: `mount -o remount,rw /`
5. Cambiar la contraseña: `passwd luiisao20`
6. Reiniciar: `reboot`

## 2. Expansión del Disco Principal (LVM)
Por defecto, Ubuntu restringe la partición principal a 100GB. Para usar todo el Terabyte disponible:
1. Comprobar espacio libre: `sudo vgdisplay`
2. Extender volumen: `sudo lvextend -l +100%FREE /dev/mapper/ubuntu--vg-ubuntu--lv`
3. Aplicar cambios: `sudo resize2fs /dev/mapper/ubuntu--vg-ubuntu--lv`

## 3. Instalación de SSH y Docker
**Habilitar acceso remoto (SSH):**
```bash
sudo apt update
sudo apt install openssh-server -y
sudo systemctl enable --now ssh
sudo ufw allow ssh
```
*Conexión desde PC principal:* `ssh luiisao20@<IP_DEL_SERVIDOR>`

**Instalar Docker:**
```bash
sudo apt install ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo "deb [arch=amd64 signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu noble stable" | sudo tee /etc/apt/sources.list.d/docker.list

sudo apt update
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin -y
sudo usermod -aG docker $USER
```
*(Reiniciar la sesión SSH para aplicar permisos de Docker).*

## 4. Despliegue de Servicios (PostgreSQL y MinIO)
Los servicios se levantan usando `docker-compose up -d`. 

**⚠️ NOTA IMPORTANTE SOBRE HARDWARE (MinIO):**
El servidor es una laptop Sony Vaio antigua que **no soporta el conjunto de instrucciones `x86-64-v2`** requeridas por las versiones recientes de MinIO. 
Por lo tanto, es estrictamente necesario anclar la versión de MinIO en el `docker-compose.yml` a la release de Agosto de 2023:
```yaml
image: minio/minio:RELEASE.2023-08-31T15-31-16Z
```
Si se usa `latest` o una versión de 2024, el contenedor fallará en un bucle infinito con el error: `Fatal glibc error: CPU does not support x86-64-v2`.

## 5. Configuración de Red Segura y HTTPS (Tailscale)
Tailscale permite acceder al servidor desde cualquier lugar sin abrir puertos en el router, y provee certificados HTTPS gratuitos.

**Para asegurar que Tailscale inicie automáticamente con el sistema:**
1. Habilitar y arrancar el demonio:
   ```bash
   sudo systemctl enable tailscaled
   sudo systemctl start tailscaled
   ```
2. Autenticar la sesión a nivel de sistema:
   ```bash
   sudo tailscale up
   ```
3. **CRÍTICO:** Ir a la [consola de administración de Tailscale](https://login.tailscale.com/admin/machines), buscar el servidor (Sony Vaio), hacer clic en los tres puntos y seleccionar **Disable key expiry**. Esto evita que el servidor se desconecte cada 180 días de forma silenciosa.

**Para habilitar HTTPS:**
1. Habilitar **MagicDNS** y **HTTPS Certificates** en la pestaña DNS de la consola web de Tailscale.
2. Identificar el dominio asignado: `tailscale status` (ej. `mi-servidor.cola-zorro.ts.net`).
3. Generar los certificados en el servidor:
   ```bash
   sudo tailscale cert mi-servidor.cola-zorro.ts.net
   ```
