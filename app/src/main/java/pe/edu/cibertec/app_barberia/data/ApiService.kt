
package pe.edu.cibertec.app_barberia.data

import pe.edu.cibertec.app_barberia.login.LoginRequest
import pe.edu.cibertec.app_barberia.login.UsuarioResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.ResponseBody

interface ApiService {

    // LOGIN
    @POST("api/auth/login")
    fun iniciarSesion(
        @Body solicitud: LoginRequest
    ): Call<UsuarioResponse>

    // SERVICIOS
    @GET("api/servicios")
    fun listarServicios(): Call<List<com.google.gson.JsonObject>>

    // BARBEROS
    @GET("api/barberos")
    fun listarBarberos(): Call<List<com.google.gson.JsonObject>>

    // CITAS DE UN USUARIO
    @GET("api/citas/usuario/{idUsuario}")
    fun listarCitasUsuario(
        @Path("idUsuario") idUsuario: Int
    ): Call<List<com.google.gson.JsonObject>>

    // RESERVAR CITA
    @POST("api/citas/reservar")
    fun reservarCita(
        @Body solicitud: com.google.gson.JsonObject
    ): Call<com.google.gson.JsonObject>

    // MODIFICAR CITA
    @PUT("api/citas/modificar/{idCita}")
    fun modificarCita(
        @Path("idCita") idCita: Int,
        @Body solicitud: com.google.gson.JsonObject
    ): Call<com.google.gson.JsonObject>

    // CANCELAR CITA
    @PUT("api/citas/cancelar/{idCita}")
    fun cancelarCita(
        @Path("idCita") idCita: Int
    ): Call<com.google.gson.JsonObject>

    // REGISTRAR USUARIO
    @POST("api/auth/registro")
    fun registrarUsuario(
        @Body solicitud: pe.edu.cibertec.app_barberia.login.RegistroRequest
    ): Call<ResponseBody>

    // OBTENER PERFIL
    @GET("api/usuarios/{id}")
    fun obtenerPerfil(
        @Path("id") id: Int
    ): Call<com.google.gson.JsonObject>

    // ACTUALIZAR DATOS PERSONALES
    @PUT("api/usuarios/perfil/{id}")
    fun actualizarPerfil(
        @Path("id") id: Int,
        @Body datos: com.google.gson.JsonObject
    ): Call<com.google.gson.JsonObject>

    // CAMBIAR CONTRASEÑA
    @PUT("api/usuarios/password/{id}")
    fun cambiarPassword(
        @Path("id") id: Int,
        @Body datos: com.google.gson.JsonObject
    ): Call<com.google.gson.JsonObject>

    // SUBIR FOTO DE PERFIL
    @Multipart
    @POST("api/usuarios/{id}/foto")
    fun subirFotoPerfil(
        @Path("id") id: Int,
        @Part foto: okhttp3.MultipartBody.Part
    ): Call<com.google.gson.JsonObject>
}