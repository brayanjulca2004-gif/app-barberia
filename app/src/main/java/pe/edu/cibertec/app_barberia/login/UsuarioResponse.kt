package pe.edu.cibertec.app_barberia.login

data class UsuarioResponse(
    val idUsuario: Int,
    val nombre: String,
    val correo: String,
    val telefono: String?,
    val rol: String,
    val fotoUrl: String?,
    val estado: String
)