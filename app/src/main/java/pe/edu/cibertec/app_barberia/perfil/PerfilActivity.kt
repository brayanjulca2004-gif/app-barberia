package pe.edu.cibertec.app_barberia.perfil

import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class PerfilActivity : AppCompatActivity() {

    private lateinit var imgFotoPerfil: ImageView
    private lateinit var etNombrePerfil: EditText
    private lateinit var etCorreoPerfil: EditText
    private lateinit var etTelefonoPerfil: EditText
    private lateinit var etPasswordActual: EditText
    private lateinit var etPasswordNueva: EditText
    private lateinit var etPasswordConfirmar: EditText
    private lateinit var tvEstadoPerfil: TextView
    private lateinit var tvEstadoFoto: TextView
    private lateinit var tvEstadoDatos: TextView
    private lateinit var tvEstadoPassword: TextView
    private lateinit var btnCambiarFoto: Button
    private lateinit var btnGuardarPerfil: Button
    private lateinit var btnCambiarPassword: Button

    private var idUsuario = -1
    private var llamadaPerfil: Call<JsonObject>? = null
    private var llamadaGuardar: Call<JsonObject>? = null
    private var llamadaPassword: Call<JsonObject>? = null
    private var llamadaFoto: Call<JsonObject>? = null

    private val selectorFoto = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (
            uri != null &&
            idUsuario > 0 &&
            ::imgFotoPerfil.isInitialized
        ) {
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(imgFotoPerfil)

            subirFoto(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil)
        configurarBarrasSistema()

        imgFotoPerfil = findViewById(R.id.imgFotoPerfil)
        etNombrePerfil = findViewById(R.id.etNombrePerfil)
        etCorreoPerfil = findViewById(R.id.etCorreoPerfil)
        etTelefonoPerfil = findViewById(R.id.etTelefonoPerfil)
        etPasswordActual = findViewById(R.id.etPasswordActual)
        etPasswordNueva = findViewById(R.id.etPasswordNueva)
        etPasswordConfirmar = findViewById(R.id.etPasswordConfirmar)
        tvEstadoPerfil = findViewById(R.id.tvEstadoPerfil)
        tvEstadoFoto = findViewById(R.id.tvEstadoFoto)
        tvEstadoDatos = findViewById(R.id.tvEstadoDatos)
        tvEstadoPassword = findViewById(R.id.tvEstadoPassword)

        btnCambiarFoto = findViewById(R.id.btnCambiarFoto)
        btnGuardarPerfil = findViewById(R.id.btnGuardarPerfil)
        btnCambiarPassword = findViewById(R.id.btnCambiarPassword)

        idUsuario = getSharedPreferences(
            "BarberApp",
            MODE_PRIVATE
        ).getInt("idUsuario", -1)

        findViewById<TextView>(
            R.id.tvVolverPerfil
        ).setOnClickListener {
            finish()
        }

        btnCambiarFoto.setOnClickListener {
            selectorFoto.launch("image/*")
        }

        btnGuardarPerfil.setOnClickListener {
            guardarPerfil()
        }

        btnCambiarPassword.setOnClickListener {
            cambiarPassword()
        }

        if (idUsuario <= 0) {
            mostrarEstado(
                tvEstadoPerfil,
                "No se encontró la sesión del usuario."
            )

            btnCambiarFoto.isEnabled = false
            btnGuardarPerfil.isEnabled = false
            btnCambiarPassword.isEnabled = false
            return
        }

        val fotoGuardada = getSharedPreferences(
            "BarberApp",
            MODE_PRIVATE
        ).getString("fotoUrl", null)

        if (!fotoGuardada.isNullOrBlank()) {
            mostrarFoto(fotoGuardada)
        }

        cargarPerfil()
    }

    private fun configurarBarrasSistema() {
        val principal = findViewById<View>(R.id.main)
        val barraInferior = findViewById<View>(
            R.id.bottomNavContainer
        )

        val alturaBase = dp(78)

        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }

        ViewCompat.setOnApplyWindowInsetsListener(
            principal
        ) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.setPadding(
                barras.left,
                barras.top,
                barras.right,
                0
            )

            val nuevaAltura = alturaBase + barras.bottom
            val parametros = barraInferior.layoutParams

            if (parametros.height != nuevaAltura) {
                parametros.height = nuevaAltura
                barraInferior.layoutParams = parametros
            }

            barraInferior.setPadding(
                0, 0, 0, barras.bottom
            )
            barraInferior.setBackgroundColor(Color.WHITE)

            insets
        }
    }

    private fun cargarPerfil() {
        mostrarEstado(
            tvEstadoPerfil,
            "Cargando perfil..."
        )

        llamadaPerfil = RetrofitClient.api.obtenerPerfil(
            idUsuario
        )

        llamadaPerfil?.enqueue(
            object : Callback<JsonObject> {

                override fun onResponse(
                    call: Call<JsonObject>,
                    response: Response<JsonObject>
                ) {
                    if (
                        isFinishing ||
                        isDestroyed ||
                        call.isCanceled
                    ) return

                    if (!response.isSuccessful) {
                        mostrarEstado(
                            tvEstadoPerfil,
                            "No se pudo cargar el perfil (${response.code()})."
                        )
                        return
                    }

                    val datos = response.body()

                    if (datos == null) {
                        mostrarEstado(
                            tvEstadoPerfil,
                            "No se encontraron los datos del perfil."
                        )
                        return
                    }

                    etNombrePerfil.setText(
                        obtenerTexto(datos, "nombre")
                    )

                    etCorreoPerfil.setText(
                        obtenerTexto(datos, "correo")
                    )

                    etTelefonoPerfil.setText(
                        obtenerTexto(datos, "telefono")
                    )

                    val foto = obtenerTexto(
                        datos,
                        "fotoUrl"
                    )

                    if (foto.isNotBlank()) {
                        getSharedPreferences(
                            "BarberApp",
                            MODE_PRIVATE
                        ).edit()
                            .putString("fotoUrl", foto)
                            .apply()

                        mostrarFoto(foto)
                    }

                    tvEstadoPerfil.visibility = View.GONE
                }

                override fun onFailure(
                    call: Call<JsonObject>,
                    t: Throwable
                ) {
                    if (
                        isFinishing ||
                        isDestroyed ||
                        call.isCanceled
                    ) return

                    mostrarEstado(
                        tvEstadoPerfil,
                        "No se pudo conectar con el servidor."
                    )
                }
            }
        )
    }

    private fun guardarPerfil() {
        val nombre = etNombrePerfil.text.toString().trim()
        val correo = etCorreoPerfil.text.toString().trim()
        val telefono = etTelefonoPerfil.text.toString().trim()

        if (nombre.isEmpty() || correo.isEmpty()) {
            mostrarEstado(
                tvEstadoDatos,
                "Completa el nombre y el correo."
            )
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            etCorreoPerfil.error = "Ingresa un correo válido"

            mostrarEstado(
                tvEstadoDatos,
                "Ingresa un correo electrónico válido."
            )
            return
        }

        val datos = JsonObject().apply {
            addProperty("nombre", nombre)
            addProperty("correo", correo)
            addProperty("telefono", telefono)
        }

        btnGuardarPerfil.isEnabled = false

        mostrarEstado(
            tvEstadoDatos,
            "Guardando cambios..."
        )

        llamadaGuardar = RetrofitClient.api.actualizarPerfil(
            idUsuario,
            datos
        )

        llamadaGuardar?.enqueue(
            object : Callback<JsonObject> {

                override fun onResponse(
                    call: Call<JsonObject>,
                    response: Response<JsonObject>
                ) {
                    if (
                        isFinishing ||
                        isDestroyed ||
                        call.isCanceled
                    ) return

                    btnGuardarPerfil.isEnabled = true

                    if (response.isSuccessful) {
                        getSharedPreferences(
                            "BarberApp",
                            MODE_PRIVATE
                        ).edit()
                            .putString("nombreUsuario", nombre)
                            .putString("correoUsuario", correo)
                            .apply()

                        mostrarEstado(
                            tvEstadoDatos,
                            "Datos actualizados correctamente."
                        )
                    } else {
                        mostrarEstado(
                            tvEstadoDatos,
                            "No se pudo guardar (${response.code()}). ${mensajeError(response)}"
                        )
                    }
                }

                override fun onFailure(
                    call: Call<JsonObject>,
                    t: Throwable
                ) {
                    if (
                        isFinishing ||
                        isDestroyed ||
                        call.isCanceled
                    ) return

                    btnGuardarPerfil.isEnabled = true

                    mostrarEstado(
                        tvEstadoDatos,
                        "No se pudo conectar con el servidor."
                    )
                }
            }
        )
    }

    private fun cambiarPassword() {
        val actual = etPasswordActual.text.toString()
        val nueva = etPasswordNueva.text.toString()
        val confirmar = etPasswordConfirmar.text.toString()

        if (
            actual.isEmpty() ||
            nueva.isEmpty() ||
            confirmar.isEmpty()
        ) {
            mostrarEstado(
                tvEstadoPassword,
                "Completa los tres campos de contraseña."
            )
            return
        }

        if (nueva.length < 8) {
            mostrarEstado(
                tvEstadoPassword,
                "La nueva contraseña debe tener al menos 8 caracteres."
            )
            return
        }

        if (nueva != confirmar) {
            mostrarEstado(
                tvEstadoPassword,
                "Las nuevas contraseñas no coinciden."
            )
            return
        }

        val datos = JsonObject().apply {
            addProperty("passwordActual", actual)
            addProperty("passwordNueva", nueva)
            addProperty("passwordConfirmar", confirmar)
        }

        btnCambiarPassword.isEnabled = false

        mostrarEstado(
            tvEstadoPassword,
            "Actualizando contraseña..."
        )

        llamadaPassword = RetrofitClient.api.cambiarPassword(
            idUsuario,
            datos
        )

        llamadaPassword?.enqueue(
            object : Callback<JsonObject> {

                override fun onResponse(
                    call: Call<JsonObject>,
                    response: Response<JsonObject>
                ) {
                    if (
                        isFinishing ||
                        isDestroyed ||
                        call.isCanceled
                    ) return

                    btnCambiarPassword.isEnabled = true

                    if (response.isSuccessful) {
                        etPasswordActual.text.clear()
                        etPasswordNueva.text.clear()
                        etPasswordConfirmar.text.clear()

                        mostrarEstado(
                            tvEstadoPassword,
                            "Contraseña actualizada correctamente."
                        )
                    } else {
                        mostrarEstado(
                            tvEstadoPassword,
                            "No se pudo cambiar la contraseña (${response.code()}). ${mensajeError(response)}"
                        )
                    }
                }

                override fun onFailure(
                    call: Call<JsonObject>,
                    t: Throwable
                ) {
                    if (
                        isFinishing ||
                        isDestroyed ||
                        call.isCanceled
                    ) return

                    btnCambiarPassword.isEnabled = true

                    mostrarEstado(
                        tvEstadoPassword,
                        "No se pudo conectar con el servidor."
                    )
                }
            }
        )
    }

    private fun subirFoto(uri: Uri) {
        var archivo: File? = null

        try {
            archivo = copiarUriAArchivo(uri)

            val tipo = contentResolver.getType(uri)
                ?: "image/jpeg"

            val cuerpo = archivo.asRequestBody(
                tipo.toMediaTypeOrNull()
            )

            val parte = MultipartBody.Part.createFormData(
                "foto",
                archivo.name,
                cuerpo
            )

            val archivoTemporal = archivo

            btnCambiarFoto.isEnabled = false

            mostrarEstado(
                tvEstadoFoto,
                "Subiendo foto..."
            )

            llamadaFoto = RetrofitClient.api.subirFotoPerfil(
                idUsuario,
                parte
            )

            llamadaFoto?.enqueue(
                object : Callback<JsonObject> {

                    override fun onResponse(
                        call: Call<JsonObject>,
                        response: Response<JsonObject>
                    ) {
                        archivoTemporal.delete()

                        if (
                            isFinishing ||
                            isDestroyed ||
                            call.isCanceled
                        ) return

                        btnCambiarFoto.isEnabled = true

                        if (response.isSuccessful) {
                            val foto = response.body()?.let {
                                obtenerTexto(it, "fotoUrl")
                            }.orEmpty()

                            if (foto.isNotBlank()) {
                                getSharedPreferences(
                                    "BarberApp",
                                    MODE_PRIVATE
                                ).edit()
                                    .putString("fotoUrl", foto)
                                    .apply()

                                mostrarFoto(foto)
                            }

                            mostrarEstado(
                                tvEstadoFoto,
                                "Foto actualizada correctamente."
                            )
                        } else {
                            mostrarEstado(
                                tvEstadoFoto,
                                "No se pudo subir la foto (${response.code()}). ${mensajeError(response)}"
                            )
                        }
                    }

                    override fun onFailure(
                        call: Call<JsonObject>,
                        t: Throwable
                    ) {
                        archivoTemporal.delete()

                        if (
                            isFinishing ||
                            isDestroyed ||
                            call.isCanceled
                        ) return

                        btnCambiarFoto.isEnabled = true

                        mostrarEstado(
                            tvEstadoFoto,
                            "No se pudo subir la fotografía."
                        )
                    }
                }
            )
        } catch (_: Exception) {
            archivo?.delete()
            btnCambiarFoto.isEnabled = true

            mostrarEstado(
                tvEstadoFoto,
                "No se pudo leer la imagen seleccionada."
            )
        }
    }

    private fun mostrarFoto(url: String) {
        val urlFinal =
            if (
                url.startsWith("http://") ||
                url.startsWith("https://")
            ) {
                url
            } else {
                "https://storage.googleapis.com/barberapp-storage-2026/usuarios/$url"
            }

        Glide.with(this)
            .load(urlFinal)
            .circleCrop()
            .placeholder(R.drawable.logo_barberapp)
            .error(R.drawable.logo_barberapp)
            .into(imgFotoPerfil)
    }

    private fun copiarUriAArchivo(uri: Uri): File {
        val nombre = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        } ?: "foto_perfil.jpg"

        val extension = nombre
            .substringAfterLast('.', "jpg")
            .replace(
                Regex("[^A-Za-z0-9]"),
                ""
            )
            .ifBlank { "jpg" }

        val archivo = File(
            cacheDir,
            "perfil_${System.currentTimeMillis()}.$extension"
        )

        contentResolver.openInputStream(uri).use { entrada ->
            requireNotNull(entrada) {
                "No se pudo abrir la imagen."
            }

            FileOutputStream(archivo).use { salida ->
                entrada.copyTo(salida)
            }
        }

        return archivo
    }

    private fun obtenerTexto(
        datos: JsonObject,
        clave: String
    ): String {
        val valor = datos.get(clave)

        return if (
            valor == null ||
            valor.isJsonNull
        ) {
            ""
        } else {
            try {
                valor.asString
            } catch (_: Exception) {
                ""
            }
        }
    }

    private fun mensajeError(
        response: Response<JsonObject>
    ): String {
        return try {
            response.errorBody()
                ?.string()
                ?.let {
                    org.json.JSONObject(it)
                        .optString("mensaje")
                }
                ?.takeIf { it.isNotBlank() }
                ?: "Revisa los datos e inténtalo nuevamente."
        } catch (_: Exception) {
            "Revisa los datos e inténtalo nuevamente."
        }
    }

    private fun mostrarEstado(
        vista: TextView,
        mensaje: String
    ) {
        vista.text = mensaje
        vista.visibility = View.VISIBLE
    }

    private fun dp(valor: Int): Int {
        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }

    override fun onDestroy() {
        llamadaPerfil?.cancel()
        llamadaGuardar?.cancel()
        llamadaPassword?.cancel()
        llamadaFoto?.cancel()
        super.onDestroy()
    }
}