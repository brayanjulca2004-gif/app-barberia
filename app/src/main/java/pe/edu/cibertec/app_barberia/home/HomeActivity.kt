package pe.edu.cibertec.app_barberia.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import pe.edu.cibertec.app_barberia.login.MainActivity
import pe.edu.cibertec.app_barberia.servicios.ServiciosActivity
import pe.edu.cibertec.app_barberia.barberos.BarberosActivity
import pe.edu.cibertec.app_barberia.citas.ReservarCitaActivity
import pe.edu.cibertec.app_barberia.citas.MisCitasActivity
import pe.edu.cibertec.app_barberia.perfil.PerfilActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val main = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvSaludo = findViewById(R.id.tvSaludo)
        tvEstado = findViewById(R.id.tvEstadoProximasCitas)
        rvProximasCitas = findViewById(R.id.rvProximasCitas)
        ivFotoPerfil = findViewById(R.id.ivHomeFotoPerfil)
        cardAvatar = findViewById(R.id.cardHomeAvatar)

        // Al tocar la foto en inicio, abre el perfil
        cardAvatar.setOnClickListener {
            abrirPantalla(PerfilActivity::class.java)
        }

        adapter = ProximasCitasAdapter(mutableListOf())
        rvProximasCitas.layoutManager = LinearLayoutManager(this)
        rvProximasCitas.adapter = adapter

        // TARJETAS PRINCIPALES
        findViewById<View>(R.id.cardServicios).setOnClickListener { abrirPantalla(ServiciosActivity::class.java) }
        findViewById<View>(R.id.cardBarberos).setOnClickListener { abrirPantalla(BarberosActivity::class.java) }
        findViewById<View>(R.id.cardReservar).setOnClickListener { abrirPantalla(ReservarCitaActivity::class.java) }
        findViewById<View>(R.id.cardMisCitas).setOnClickListener { abrirPantalla(MisCitasActivity::class.java) }



        // CERRAR SESIÓN
        findViewById<TextView>(R.id.tvBack).setOnClickListener { cerrarSesion() }
    }

    override fun onResume() {
        super.onResume()

        val preferencias = getSharedPreferences("BarberApp", MODE_PRIVATE)
        val nombre = preferencias.getString("nombreUsuario", "Usuario")
        val foto = preferencias.getString("fotoUsuario", null)
        val idUsuario = preferencias.getInt("idUsuario", -1)

        tvSaludo.text = "Hola, $nombre"

        // Cargar foto de perfil en el avatar
        cargarFotoPerfil(foto, idUsuario)

        cargarProximasCitas()
    }

    private fun cargarFotoPerfil(fotoGuardada: String?, idUsuario: Int) {
        val preferencias = getSharedPreferences("BarberApp", MODE_PRIVATE)

        // 1. Obtener la URL guardada bajo la clave que usa PerfilActivity ("fotoUrl")
        val fotoLocal = if (!fotoGuardada.isNullOrBlank()) {
            fotoGuardada
        } else {
            preferencias.getString("fotoUrl", null)
        }

        // Si ya está en SharedPreferences, la cargamos de inmediato
        if (!fotoLocal.isNullOrBlank()) {
            mostrarImagenConGlide(fotoLocal)
        }

        // 2. Si no hay foto local o para refrescar la última foto de la nube:
        if (idUsuario > 0) {
            RetrofitClient.api.obtenerPerfil(idUsuario).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (isFinishing || isDestroyed) return

                    if (response.isSuccessful && response.body() != null) {
                        val datos = response.body()!!
                        val foto = datos.get("fotoUrl")

                        if (foto != null && !foto.isJsonNull && foto.asString.isNotBlank()) {
                            val urlNube = foto.asString

                            // Guardar con la misma clave exacta que usa PerfilActivity
                            preferencias.edit()
                                .putString("fotoUrl", urlNube)
                                .apply()

                            mostrarImagenConGlide(urlNube)
                        }
                    }
                }

                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    // Mantiene la imagen local o el placeholder si falla la red
                }
            })
        }
    }

    private fun mostrarImagenConGlide(url: String) {
        // En tu backend 'fotoUrl' ya viene como URL completa (http...)
        val urlFinal = if (url.startsWith("http")) {
            url
        } else {
            "https://storage.googleapis.com/barberapp-storage-2026/usuarios/$url"
        }

        ivFotoPerfil.imageTintList = null
        Glide.with(this)
            .load(urlFinal)
            .circleCrop()
            .placeholder(R.drawable.logo_barberapp)
            .error(R.drawable.logo_barberapp)
            .into(ivFotoPerfil)
    }

    private fun cargarProximasCitas() {
        val preferencias = getSharedPreferences("BarberApp", MODE_PRIVATE)
        val idUsuario = preferencias.getInt("idUsuario", -1)

        llamadaCitas?.cancel()
        adapter.actualizar(emptyList())

        if (idUsuario <= 0) {
            mostrarEstado("Inicia sesión para consultar tus citas.")
            return
        }

        mostrarEstado("Consultando tus citas...")

        llamadaCitas = RetrofitClient.api.listarCitasUsuario(idUsuario)
        llamadaCitas?.enqueue(object : Callback<List<JsonObject>> {
            override fun onResponse(
                call: Call<List<JsonObject>>,
                response: Response<List<JsonObject>>
            ) {
                if (isFinishing || isDestroyed) return

                if (!response.isSuccessful) {
                    mostrarEstado("No se pudieron cargar tus citas.")
                    return
                }

                val ahora = Date()
                val formatoFechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                val proximas = (response.body() ?: emptyList())
                    .mapNotNull { cita ->
                        val estado = obtenerTexto(cita, "estado")
                        if (!estado.equals("PENDIENTE", true) && !estado.equals("CONFIRMADA", true)) {
                            return@mapNotNull null
                        }

                        try {
                            val strFecha = obtenerTexto(cita, "fecha")
                            val strHora = obtenerTexto(cita, "hora")
                            val fechaHoraCita = formatoFechaHora.parse("$strFecha $strHora")

                            if (fechaHoraCita != null && fechaHoraCita.after(ahora)) {
                                Pair(cita, fechaHoraCita)
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
                    mostrarEstado("No tienes citas próximas pendientes o confirmadas.")
                } else {
                    tvEstado.visibility = View.GONE
                    adapter.actualizar(proximas)
                }
            }

            override fun onFailure(call: Call<List<JsonObject>>, t: Throwable) {
                if (call.isCanceled || isFinishing || isDestroyed) return
                mostrarEstado("No se pudo conectar con el servidor. Comprueba tu conexión.")
            }
        })
    }

    private fun obtenerTexto(cita: JsonObject, clave: String): String {
        val valor = cita.get(clave)
        return if (valor == null || valor.isJsonNull) "" else valor.asString
    }

    private fun mostrarEstado(mensaje: String) {
        tvEstado.text = mensaje
        tvEstado.visibility = View.VISIBLE
    }

    private fun abrirPantalla(destino: Class<*>) {
        startActivity(Intent(this, destino))
    }

    private fun cerrarSesion() {
        getSharedPreferences("BarberApp", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        llamadaCitas?.cancel()
        super.onDestroy()
    }
}