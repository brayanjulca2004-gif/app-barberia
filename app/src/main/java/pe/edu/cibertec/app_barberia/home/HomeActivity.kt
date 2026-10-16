package pe.edu.cibertec.app_barberia.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class HomeActivity : AppCompatActivity() {
    private lateinit var tvEstado: TextView
    private lateinit var tvSaludo: TextView
    private lateinit var rvProximasCitas: RecyclerView
    private lateinit var adapter: ProximasCitasAdapter

    private var llamadaCitas: Call<List<JsonObject>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val main = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        tvSaludo = findViewById(R.id.tvSaludo)
        tvEstado = findViewById(R.id.tvEstadoProximasCitas)
        rvProximasCitas = findViewById(R.id.rvProximasCitas)

        adapter = ProximasCitasAdapter(mutableListOf())
        rvProximasCitas.layoutManager = LinearLayoutManager(this)
        rvProximasCitas.adapter = adapter

        // TARJETAS PRINCIPALES

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

        // BARRA INFERIOR

        findViewById<View>(R.id.navInicio).setOnClickListener {}

        findViewById<View>(R.id.navServicios).setOnClickListener {
            abrirPantalla(ServiciosActivity::class.java)
        }

        findViewById<View>(R.id.navBarberos).setOnClickListener {
            abrirPantalla(BarberosActivity::class.java)
        }

        findViewById<View>(R.id.navCitas).setOnClickListener {
            abrirPantalla(MisCitasActivity::class.java)
        }

        findViewById<View>(R.id.navPerfil).setOnClickListener {
            abrirPantalla(PerfilActivity::class.java)
        }

        // CERRAR SESIÓN

        findViewById<TextView>(R.id.tvBack).setOnClickListener {
            cerrarSesion()
        }
    }

    override fun onResume() {
        super.onResume()

        val preferencias =
            getSharedPreferences("BarberApp", MODE_PRIVATE)

        val nombre =
            preferencias.getString("nombreUsuario", "Usuario")

        tvSaludo.text = "Hola, $nombre"

        cargarProximasCitas()
    }

    private fun cargarProximasCitas() {
        val preferencias =
            getSharedPreferences("BarberApp", MODE_PRIVATE)

        val idUsuario = preferencias.getInt("idUsuario", -1)

        llamadaCitas?.cancel()
        adapter.actualizar(emptyList())

        if (idUsuario <= 0) {
            mostrarEstado("Inicia sesión para consultar tus citas.")
            return
        }

        mostrarEstado("Consultando tus citas...")

        llamadaCitas =
            RetrofitClient.api.listarCitasUsuario(idUsuario)

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

                val ahora = LocalDateTime.now()

                val proximas = (response.body() ?: emptyList())
                    .mapNotNull { cita ->

                        val estado = obtenerTexto(cita, "estado")

                        // Mostrar citas pendientes o confirmadas.
                        if (
                            !estado.equals("PENDIENTE", true) &&
                            !estado.equals("CONFIRMADA", true)
                        ) {
                            return@mapNotNull null
                        }

                        try {
                            val fecha = LocalDate.parse(
                                obtenerTexto(cita, "fecha")
                            )

                            val hora = LocalTime.parse(
                                obtenerTexto(cita, "hora")
                            )

                            val fechaHora =
                                LocalDateTime.of(fecha, hora)

                            if (fechaHora.isAfter(ahora)) {
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
                if (call.isCanceled || isFinishing || isDestroyed) {
                    return
                }

                mostrarEstado(
                    "No se pudo conectar con el servidor. Comprueba tu conexión."
                )
            }
        })
    }

    private fun obtenerTexto(
        cita: JsonObject,
        clave: String
    ): String {
        val valor = cita.get(clave)

        return if (valor == null || valor.isJsonNull) {
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
        startActivity(Intent(this, destino))
    }

    private fun cerrarSesion() {
        getSharedPreferences("BarberApp", MODE_PRIVATE)
            .edit()
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
        super.onDestroy()
    }
}