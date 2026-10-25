# Integración continua con Jenkins

Jenkins descarga el código de GitHub, ejecuta Maven Wrapper y conserva las pruebas
y el JAR de las construcciones correctas. No despliega ni arranca la aplicación.

## Arranque local

Con Docker Desktop arrancado, desde la raíz:

```sh
docker compose -f compose.jenkins.yaml up -d
```

Abre http://localhost:8081. El puerto 8080 sigue disponible para la aplicación.
Obtén la contraseña inicial en tu terminal (no la publiques):

```sh
docker compose -f compose.jenkins.yaml exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Desbloquea Jenkins, instala los plugins sugeridos y crea tu usuario administrador.
Se necesitan los plugins Pipeline, Git y JUnit. Mantén activada la autenticación.

Este entorno didáctico ejecuta las construcciones en el nodo integrado de Jenkins,
con Java 21. Maven compila el proyecto para Java 17, como indica pom.xml; el runtime
de estas pruebas de CI es Java 21. Para un servidor compartido se separarían los
agentes de construcción del controlador. No hay acceso al Docker del equipo desde
el contenedor ni se monta el directorio del proyecto o su base H2.

## Publicar la configuración y crear el trabajo

Primero revisa, guarda en un commit y sube Jenkinsfile a GitHub. Jenkins lee la
versión del repositorio remoto, no los cambios locales todavía sin publicar.

En Jenkins:

1. Pulsa **New Item**, nombre `banking-operations-system`, tipo **Pipeline**.
2. En **Pipeline**, elige **Pipeline script from SCM** y SCM **Git**.
3. URL: `https://github.com/daridoto/banking_operations_system.git`.
4. Rama: `*/main`. Script Path: `Jenkinsfile`.
5. Si el repositorio es público, no hacen falta credenciales para leerlo. Si es
   privado, configúralas en el almacén de credenciales de Jenkins, no en el código.
6. Guarda y pulsa **Build Now** para la primera ejecución.

El checkout de Git es automático en este tipo de Pipeline. Después de la primera
ejecución, Jenkins consulta GitHub cada cinco minutos y ejecuta el trabajo cuando
detecta cambios. Esto funciona mientras Jenkins y Docker estén arrancados, sin
publicar Jenkins en Internet ni configurar webhooks.

## Qué comprobar

- **Console Output**: Maven termina con `BUILD SUCCESS`.
- **Test Result**: pruebas ejecutadas, sin fallos (actualmente 51).
- **Artifacts**: JAR descargable, asociado a esa ejecución y al commit construido.
- Si las pruebas fallan, la ejecución falla y no se archiva un nuevo JAR.

`clean verify` limpia los resultados anteriores, compila, prueba y empaqueta.
El bloque `post` publica informes incluso cuando fallan pruebas. Si Maven falla
antes de producir informes, puede aparecer también el aviso de informes ausentes.
La compilación no cambia saldos de la demo: las pruebas usan sus propias bases.

## Parar y continuar

```sh
docker compose -f compose.jenkins.yaml stop
docker compose -f compose.jenkins.yaml start
```

El volumen `jenkins_home` conserva usuarios, trabajos y resultados. No uses
`down -v` si quieres conservarlo. Se retienen diez ejecuciones y los artefactos de
las cinco últimas. La imagen `lts-jdk21` sigue la línea LTS; una futura descarga
puede actualizar Jenkins.

Referencias: [Jenkins en Docker](https://www.jenkins.io/doc/book/installing/docker/)
y [Pipeline](https://www.jenkins.io/doc/book/pipeline/).
