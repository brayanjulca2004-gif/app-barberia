
package pe.edu.cibertec.app_barberia.servicios

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.JsonObject
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ServiciosActivity : AppCompatActivity() {

    private lateinit var listaServicios: LinearLayout
    private lateinit var progreso: ProgressBar
    private lateinit var tvEstado: TextView
    private lateinit var btnReintentar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_servicios)

        val main = findViewById<View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            v.setPadding(
                barras.left,
                barras.top,
                barras.right,
                barras.bottom
            )
            insets
        }

        listaServicios = findViewById(R.id.listaServicios)
        progreso = findViewById(R.id.progresoServicios)
        tvEstado = findViewById(R.id.tvEstadoServicios)
        btnReintentar = findViewById(R.id.btnReintentarServicios)

        findViewById<TextView>(R.id.tvBackServicios).setOnClickListener {
            finish()
        }

        btnReintentar.setOnClickListener {
            cargarServicios()
        }

        cargarServicios()
    }

    private fun cargarServicios() {
        progreso.visibility = View.VISIBLE
        tvEstado.visibility = View.VISIBLE
        tvEstado.text = "Cargando servicios..."
        listaServicios.removeAllViews()
        btnReintentar.visibility = View.GONE

        RetrofitClient.api.listarServicios()
            .enqueue(object : Callback<List<JsonObject>> {

                override fun onResponse(
                    call: Call<List<JsonObject>>,
                    response: Response<List<JsonObject>>
                ) {
                    progreso.visibility = View.GONE

                    if (response.isSuccessful) {
                        val servicios = response.body().orEmpty()

                        if (servicios.isEmpty()) {
                            tvEstado.text =
                                "Todavía no hay servicios registrados."
                            tvEstado.visibility = View.VISIBLE
                        } else {
                            tvEstado.visibility = View.GONE
                            servicios.forEach { servicio ->
                                agregarTarjetaServicio(servicio)
                            }
                        }
                    } else {
                        mostrarError(
                            "No se pudieron cargar los servicios " +
                                    "(HTTP ${response.code()})."
                        )
                    }
                }

                override fun onFailure(
                    call: Call<List<JsonObject>>,
                    t: Throwable
                ) {
                    progreso.visibility = View.GONE
                    mostrarError(
                        "No se pudo conectar con el servidor. " +
                                "Verifica que Spring Boot esté ejecutándose."
                    )
                }
            })
    }

    private fun agregarTarjetaServicio(servicio: JsonObject) {
        val nombre = obtenerTexto(
            servicio,
            "nombre",
            "nombreServicio",
            "nombre_servicio"
        ).ifBlank { "Servicio de barbería" }

        val descripcion = obtenerTexto(
            servicio,
            "descripcion",
            "descripcionServicio",
            "descripcion_servicio"
        )

        val precio = obtenerTexto(
            servicio,
            "precio",
            "precioServicio",
            "precio_servicio"
        )

        val duracion = obtenerTexto(
            servicio,
            "duracion",
            "duracionMinutos",
            "duracion_minutos"
        )

        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(14).toFloat()
            }
            elevation = dp(3).toFloat()
        }

        val parametros = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(14)
        }

        val tvNombre = TextView(this).apply {
            text = "✂  $nombre"
            setTextColor(Color.rgb(23, 52, 75))
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
        }

        tarjeta.addView(tvNombre)

        if (descripcion.isNotBlank()) {
            val tvDescripcion = TextView(this).apply {
                text = descripcion
                setTextColor(Color.rgb(88, 112, 135))
                textSize = 14f
                setPadding(0, dp(8), 0, dp(8))
            }
            tarjeta.addView(tvDescripcion)
        }

        val detalles = buildList {
            if (precio.isNotBlank()) {
                add("Precio: S/ $precio")
            }
            if (duracion.isNotBlank()) {
                add("Duración: $duracion min")
            }
        }

        if (detalles.isNotEmpty()) {
            val tvDetalles = TextView(this).apply {
                text = detalles.joinToString("   •   ")
                setTextColor(Color.rgb(10, 105, 155))
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
            }
            tarjeta.addView(tvDetalles)
        }

        listaServicios.addView(tarjeta, parametros)
    }

    private fun obtenerTexto(
        objeto: JsonObject,
        vararg posiblesClaves: String
    ): String {
        for (clave in posiblesClaves) {
            val valor = objeto.get(clave)

            if (valor != null && !valor.isJsonNull) {
                return try {
                    valor.asString
                } catch (_: Exception) {
                    valor.toString()
                }
            }
        }
        return ""
    }

    private fun mostrarError(mensaje: String) {
        tvEstado.text = mensaje
        tvEstado.visibility = View.VISIBLE
        btnReintentar.visibility = View.VISIBLE

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }
}