package pe.edu.cibertec.app_barberia.citas

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_citas)

        rvMisCitas = findViewById(R.id.rvMisCitas)
        tvEstado = findViewById(R.id.tvEstadoMisCitas)
        progreso = findViewById(R.id.progresoMisCitas)

        findViewById<TextView>(R.id.tvBackMisCitas).setOnClickListener {
            finish()
        }

        adapter = CitasAdapter(citas) { cita ->
            cancelarCita(cita)
        }

        rvMisCitas.layoutManager = LinearLayoutManager(this)
        rvMisCitas.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        cargarCitas()
    }

    private fun cargarCitas() {
        val preferencias = getSharedPreferences("BarberApp", MODE_PRIVATE)
        val idUsuario = preferencias.getInt("idUsuario", -1)

        if (idUsuario <= 0) {
            tvEstado.text = "Inicia sesión para consultar tus citas."
            return
        }

        progreso.visibility = View.VISIBLE
        tvEstado.text = ""

        RetrofitClient.api.listarCitasUsuario(idUsuario)
            .enqueue(object : Callback<List<JsonObject>> {

                override fun onResponse(
                    call: Call<List<JsonObject>>,
                    response: Response<List<JsonObject>>
                ) {
                    progreso.visibility = View.GONE

                    if (response.isSuccessful) {
                        val resultado = response.body() ?: emptyList()

                        android.util.Log.d("CITAS_API", resultado.toString())

                        adapter.actualizar(resultado)



                        tvEstado.text = if (resultado.isEmpty()) {
                            "Todavía no tienes citas reservadas."
                        } else {
                            ""
                        }
                    } else {
                        tvEstado.text = "No se pudieron cargar tus citas."
                    }
                }

                override fun onFailure(
                    call: Call<List<JsonObject>>,
                    t: Throwable
                ) {
                    progreso.visibility = View.GONE
                    tvEstado.text = "No se pudo conectar con el servidor."
                }
            })
    }

    private fun cancelarCita(cita: JsonObject) {
        val valor = cita.get("idCita")

        if (valor == null || valor.isJsonNull) {
            Toast.makeText(this, "No se encontró el ID de la cita", Toast.LENGTH_LONG).show()
            return
        }

        val idCita = valor.asInt

        progreso.visibility = View.VISIBLE

        RetrofitClient.api.cancelarCita(idCita)
            .enqueue(object : Callback<JsonObject> {

                override fun onResponse(
                    call: Call<JsonObject>,
                    response: Response<JsonObject>
                ) {
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
                    progreso.visibility = View.GONE
                    Toast.makeText(
                        this@MisCitasActivity,
                        "Error de conexión al cancelar",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }
}