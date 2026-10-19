package pe.edu.cibertec.app_barberia.citas

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MisCitasActivity : AppCompatActivity() {

    private lateinit var rvMisCitas: RecyclerView
    private lateinit var tvEstado: TextView
    private lateinit var progreso: ProgressBar
    private lateinit var adapter: CitasAdapter

    private val citas = mutableListOf<JsonObject>()
    private var llamadaCitas: Call<List<JsonObject>>? = null
    private var llamadaCancelacion: Call<JsonObject>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_mis_citas)
        configurarBarrasSistema()

        rvMisCitas = findViewById(R.id.rvMisCitas)
        tvEstado = findViewById(R.id.tvEstadoMisCitas)
        progreso = findViewById(R.id.progresoMisCitas)

        findViewById<TextView>(
            R.id.tvBackMisCitas
        ).setOnClickListener {
            finish()
        }

        findViewById<Button>(
            R.id.btnNuevaCitaMisCitas
        ).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ReservarCitaActivity::class.java
                )
            )
        }

        adapter = CitasAdapter(citas) { cita ->
            cancelarCita(cita)
        }

        rvMisCitas.layoutManager = LinearLayoutManager(this)
        rvMisCitas.adapter = adapter
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

    override fun onResume() {
        super.onResume()
        cargarCitas()
    }

    private fun cargarCitas() {
        val idUsuario = getSharedPreferences(
            "BarberApp",
            MODE_PRIVATE
        ).getInt("idUsuario", -1)

        llamadaCitas?.cancel()

        if (idUsuario <= 0) {
            progreso.visibility = View.GONE
            adapter.actualizar(emptyList())
            tvEstado.text = "Inicia sesión para consultar tus citas."
            tvEstado.visibility = View.VISIBLE
            return
        }

        progreso.visibility = View.VISIBLE
        tvEstado.visibility = View.VISIBLE
        tvEstado.text = "Cargando tus citas..."

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
                    ) return

                    progreso.visibility = View.GONE

                    if (response.isSuccessful) {
                        val resultado = response.body().orEmpty()

                        adapter.actualizar(resultado)

                        tvEstado.visibility =
                            if (resultado.isEmpty()) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }

                        tvEstado.text =
                            if (resultado.isEmpty()) {
                                "Todavía no tienes citas reservadas."
                            } else {
                                ""
                            }
                    } else {
                        tvEstado.text =
                            "No se pudieron cargar tus citas."
                        tvEstado.visibility = View.VISIBLE
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
                    ) return

                    progreso.visibility = View.GONE
                    tvEstado.text =
                        "No se pudo conectar con el servidor."
                    tvEstado.visibility = View.VISIBLE
                }
            }
        )
    }

    private fun cancelarCita(cita: JsonObject) {
        val valor = cita.get("idCita")

        val idCita = try {
            if (valor == null || valor.isJsonNull) {
                null
            } else {
                valor.asInt
            }
        } catch (_: Exception) {
            null
        }

        if (idCita == null) {
            Toast.makeText(
                this,
                "No se encontró el ID de la cita",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (llamadaCancelacion != null) return

        progreso.visibility = View.VISIBLE

        llamadaCancelacion = RetrofitClient.api.cancelarCita(
            idCita
        )

        llamadaCancelacion?.enqueue(
            object : Callback<JsonObject> {

                override fun onResponse(
                    call: Call<JsonObject>,
                    response: Response<JsonObject>
                ) {
                    if (isFinishing || isDestroyed) return

                    llamadaCancelacion = null
                    progreso.visibility = View.GONE

                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@MisCitasActivity,
                            "Cita cancelada correctamente",
                            Toast.LENGTH_LONG
                        ).show()

                        cargarCitas()
                    } else {
                        Toast.makeText(
                            this@MisCitasActivity,
                            "No se pudo cancelar la cita",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(
                    call: Call<JsonObject>,
                    t: Throwable
                ) {
                    if (isFinishing || isDestroyed) return

                    llamadaCancelacion = null
                    progreso.visibility = View.GONE

                    Toast.makeText(
                        this@MisCitasActivity,
                        "Error de conexión al cancelar",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    private fun dp(valor: Int): Int {
        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }

    override fun onDestroy() {
        llamadaCitas?.cancel()
        llamadaCancelacion?.cancel()
        super.onDestroy()
    }
}