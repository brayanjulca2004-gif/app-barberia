package pe.edu.cibertec.app_barberia.home

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.barberos.BarberosActivity
import pe.edu.cibertec.app_barberia.citas.MisCitasActivity
import pe.edu.cibertec.app_barberia.citas.ReservarCitaActivity
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import pe.edu.cibertec.app_barberia.login.MainActivity
import pe.edu.cibertec.app_barberia.perfil.PerfilActivity
import pe.edu.cibertec.app_barberia.servicios.ServiciosActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var tvEstado: TextView
    private lateinit var tvSaludo: TextView
    private lateinit var ivFotoPerfil: ImageView
    private lateinit var cardAvatar: CardView
    private lateinit var rvProximasCitas: RecyclerView
    private lateinit var adapter: ProximasCitasAdapter

    private var llamadaCitas: Call<List<JsonObject>>? = null
    private var llamadaPerfil: Call<JsonObject>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        configurarBarrasSistema()

        tvSaludo = findViewById(R.id.tvSaludo)
        tvEstado = findViewById(R.id.tvEstadoProximasCitas)
        ivFotoPerfil = findViewById(R.id.ivHomeFotoPerfil)
        cardAvatar = findViewById(R.id.cardHomeAvatar)
        rvProximasCitas = findViewById(R.id.rvProximasCitas)

        adapter = ProximasCitasAdapter(mutableListOf())

        rvProximasCitas.layoutManager = LinearLayoutManager(this)
        rvProximasCitas.adapter = adapter
        rvProximasCitas.isNestedScrollingEnabled = false

        cardAvatar.setOnClickListener {
            abrirPantalla(PerfilActivity::class.java)
        }

        findViewById<View>(R.id.cardServicios).setOnClickListener {
            abrirPantalla(ServiciosActivity::class.java)
        }

        findViewById<View>(R.id.cardBarberos).setOnClickListener {
            abrirPantalla(BarberosActivity::class.java)
        }

        findViewById<View>(R.id.cardReservar).setOnClickListener {
            abrirPantalla(ReservarCitaActivity::class.java)
        }

        findViewById<View>(R.id.cardMisCitas).setOnClickListener {
            abrirPantalla(MisCitasActivity::class.java)
        }

        findViewById<View>(R.id.tvBack).setOnClickListener {
            cerrarSesion()
        }
    }

    private fun configurarBarrasSistema() {
        val main = findViewById<View>(R.id.main)
        val barraInferior = findViewById<View>(
            R.id.bottomNavContainer
        )

        val alturaBase = (
                78 * resources.displayMetrics.density
                ).toInt()

        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val controlador = WindowCompat.getInsetsController(
            window,
            window.decorView
        )

        controlador.isAppearanceLightStatusBars = true
        controlador.isAppearanceLightNavigationBars = true

        ViewCompat.setOnApplyWindowInsetsListener(main) { vista, insets ->
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
                0,
                0,
                0,
                barras.bottom
            )

            barraInferior.setBackgroundColor(
                Color.WHITE
            )

            insets
        }
    }

    override fun onResume() {
        super.onResume()

        val preferencias = getSharedPreferences(
            "BarberApp",
            MODE_PRIVATE
        )

        val nombreCompleto = preferencias.getString(
            "nombreUsuario",
            "Usuario"
        ).orEmpty()

        val primerNombre = nombreCompleto
            .trim()
            .substringBefore(" ")
            .ifBlank { "Usuario" }

        tvSaludo.text = "Hola, $primerNombre"

        val foto = preferencias.getString(
            "fotoUrl",
            null
        ) ?: preferencias.getString(
            "fotoUsuario",
            null
        )

        val idUsuario = preferencias.getInt(
            "idUsuario",
            -1
        )

        cargarFotoPerfil(foto, idUsuario)
        cargarProximasCitas()
    }

    private fun cargarFotoPerfil(
        fotoGuardada: String?,
        idUsuario: Int
    ) {
        llamadaPerfil?.cancel()

        if (!fotoGuardada.isNullOrBlank()) {
            mostrarImagenConGlide(fotoGuardada)
        } else {
            Glide.with(this).clear(ivFotoPerfil)
            ivFotoPerfil.setImageResource(
                R.drawable.ic_nav_perfil
            )
        }

        if (idUsuario <= 0) return

        llamadaPerfil = RetrofitClient.api.obtenerPerfil(idUsuario)

        llamadaPerfil?.enqueue(object : Callback<JsonObject> {

            override fun onResponse(
                call: Call<JsonObject>,
                response: Response<JsonObject>
            ) {
                if (isFinishing || isDestroyed || call.isCanceled) {
                    return
                }

                if (!response.isSuccessful) return

                val datos = response.body() ?: return

                val foto = datos.get("fotoUrl")

                if (
                    foto != null &&
                    !foto.isJsonNull &&
                    foto.asString.isNotBlank()
                ) {
                    val url = foto.asString

                    getSharedPreferences(
                        "BarberApp",
                        MODE_PRIVATE
                    ).edit()
                        .putString("fotoUrl", url)
                        .apply()

                    mostrarImagenConGlide(url)
                }
            }

            override fun onFailure(
                call: Call<JsonObject>,
                t: Throwable
            ) {
            }
        })
    }

    private fun mostrarImagenConGlide(url: String) {
        val urlFinal = if (
            url.startsWith("http://") ||
            url.startsWith("https://")
        ) {
            url
        } else {
            "https://storage.googleapis.com/barberapp-storage-2026/usuarios/$url"
        }

        ivFotoPerfil.imageTintList = null

        Glide.with(this)
            .load(urlFinal)
            .circleCrop()
            .placeholder(R.drawable.ic_nav_perfil)
            .error(R.drawable.ic_nav_perfil)
            .into(ivFotoPerfil)
    }

    private fun cargarProximasCitas() {
        val preferencias = getSharedPreferences(
            "BarberApp",
            MODE_PRIVATE
        )

        val idUsuario = preferencias.getInt(
            "idUsuario",
            -1
        )

        llamadaCitas?.cancel()
        adapter.actualizar(emptyList())

        if (idUsuario <= 0) {
            mostrarEstado(
                "Inicia sesión para consultar tus citas."
            )
            return
        }

        mostrarEstado("Consultando tus citas...")

        llamadaCitas = RetrofitClient.api.listarCitasUsuario(
            idUsuario
        )

        llamadaCitas?.enqueue(
            object : Callback<List<JsonObject>> {

                override fun onResponse(
                    call: Call<List<JsonObject>>,
                    response: Response<List<JsonObject>>
                ) {
                    if (
                        isFinishing ||
                        isDestroyed ||
                        call.isCanceled
                    ) {
                        return
                    }

                    if (!response.isSuccessful) {
                        mostrarEstado(
                            "No se pudieron cargar tus citas."
                        )
                        return
                    }

                    val ahora = Date()

                    val formatoFechaHora = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.getDefault()
                    )

                    val proximas = response.body()
                        .orEmpty()
                        .mapNotNull { cita ->
                            val estado = obtenerTexto(
                                cita,
                                "estado"
                            )

                            if (
                                !estado.equals("PENDIENTE", true) &&
                                !estado.equals("CONFIRMADA", true)
                            ) {
                                return@mapNotNull null
                            }

                            try {
                                val fecha = obtenerTexto(
                                    cita,
                                    "fecha"
                                )

                                val hora = obtenerTexto(
                                    cita,
                                    "hora"
                                )

                                val fechaHora = formatoFechaHora.parse(
                                    "$fecha $hora"
                                )

                                if (
                                    fechaHora != null &&
                                    fechaHora.after(ahora)
                                ) {
                                    Pair(cita, fechaHora)
                                } else {
                                    null
                                }
                            } catch (_: Exception) {
                                null
                            }
                        }
                        .sortedBy { it.second }
                        .map { it.first }

                    if (proximas.isEmpty()) {
                        adapter.actualizar(emptyList())

                        mostrarEstado(
                            "No tienes citas próximas pendientes o confirmadas."
                        )
                    } else {
                        tvEstado.visibility = View.GONE
                        adapter.actualizar(proximas)
                    }
                }

                override fun onFailure(
                    call: Call<List<JsonObject>>,
                    t: Throwable
                ) {
                    if (
                        call.isCanceled ||
                        isFinishing ||
                        isDestroyed
                    ) {
                        return
                    }

                    mostrarEstado(
                        "No se pudo conectar con el servidor. Comprueba tu conexión."
                    )
                }
            }
        )
    }

    private fun obtenerTexto(
        cita: JsonObject,
        clave: String
    ): String {
        val valor = cita.get(clave)

        return if (
            valor == null ||
            valor.isJsonNull
        ) {
            ""
        } else {
            valor.asString
        }
    }

    private fun mostrarEstado(mensaje: String) {
        tvEstado.text = mensaje
        tvEstado.visibility = View.VISIBLE
    }

    private fun abrirPantalla(destino: Class<*>) {
        startActivity(
            Intent(this, destino)
        )
    }

    private fun cerrarSesion() {
        getSharedPreferences(
            "BarberApp",
            MODE_PRIVATE
        ).edit()
            .clear()
            .apply()

        val intent = Intent(
            this,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        llamadaCitas?.cancel()
        llamadaPerfil?.cancel()
        super.onDestroy()
    }
}