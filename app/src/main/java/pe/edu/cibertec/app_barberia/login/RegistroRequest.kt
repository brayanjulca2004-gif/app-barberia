package pe.edu.cibertec.app_barberia.login

data class RegistroRequest(
    val nombre: String,
    val correo: String,
    val password: String,
    val telefono: String
)