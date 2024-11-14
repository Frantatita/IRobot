# IRobot - Aplicación de Seguimiento de Objetos

## Descripción

**IRobot** es una aplicación móvil desarrollada en Android Studio con **Kotlin** para realizar el seguimiento de objetos mediante una placa Arduino. Esta aplicación está diseñada para controlar y monitorear el movimiento de objetos en tiempo real, utilizando la comunicación con Arduino para obtener datos de sensores. La integración de **OpenCV** está en planes futuros, lo cual permitirá mejorar la precisión en el seguimiento visual.

## Características

- **Desarrollada en Kotlin**: Aprovecha las ventajas y la eficiencia de Kotlin para el desarrollo de aplicaciones Android.
- **Integración con Arduino**: Utiliza una placa Arduino para capturar y transmitir datos de sensores.
- **Interfaz intuitiva**: Proporciona una interfaz móvil fácil de usar para el seguimiento y control de objetos.
- **Escalabilidad**: Preparada para integrar OpenCV en versiones futuras, lo que permitirá agregar capacidades de visión por computadora.

## Tecnologías utilizadas

- **Android Studio**: Entorno de desarrollo para la aplicación móvil.
- **Kotlin**: Lenguaje de programación para la aplicación Android.
- **Arduino**: Hardware para la adquisición de datos.
- **(Futuro)** OpenCV: Para el procesamiento de imágenes y seguimiento avanzado.

## Requisitos

1. **Android Studio** (última versión recomendada).
2. **Arduino IDE** para programar la placa.
3. **Dispositivo Android** con Android 8.0 o superior.
4. **Cable USB o conexión Bluetooth** para la comunicación con la placa Arduino.

## Instalación

1. Clona este repositorio:
   ```bash
   git clone https://github.com/usuario/IRobot.git
   ```

2. Abre el proyecto en Android Studio.

3. Conecta tu dispositivo Android en modo de depuración y selecciona el dispositivo en Android Studio.

4. Compila y ejecuta la aplicación.

5. Configura la placa Arduino con el código proporcionado en `arduino/` y asegúrate de que esté correctamente conectada al dispositivo Android.

## Uso

1. Ejecuta la aplicación en tu dispositivo Android.
2. Conecta el dispositivo a la placa Arduino mediante USB o Bluetooth.
3. Observa en tiempo real los datos de seguimiento del objeto a través de la interfaz de la aplicación.

