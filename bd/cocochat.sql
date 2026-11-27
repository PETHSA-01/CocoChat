-- CocoChat - Base de datos del servidor (equipo 1: usuarios)
-- No es obligatorio correrlo: el servidor crea todo solo al arrancar
-- (ConexionBD.inicializar). Sirve para crearla a mano desde phpMyAdmin
-- y como referencia para el diagrama de la documentacion.

CREATE DATABASE IF NOT EXISTS cocochat CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE cocochat;

CREATE TABLE IF NOT EXISTS usuarios (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    usuario        VARCHAR(20)  NOT NULL UNIQUE,   -- 3 a 20: letras, numeros, . y _
    contrasena     VARCHAR(100) NOT NULL,          -- texto visible (requisito de la practica)
    correo         VARCHAR(100) NOT NULL,
    pregunta       VARCHAR(200) NOT NULL,          -- pregunta de seguridad
    respuesta      VARCHAR(200) NOT NULL,          -- respuesta para recuperar credenciales
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Equipos 2 y 3: sus tablas (mensajes, amistades, grupos...) pueden hacer
-- referencia a usuarios(usuario) o usuarios(id) con FOREIGN KEY.
