
package pe.edu.cibertec.app_barberia.barberos

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.gson.JsonObject
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class BarberosActivity : AppCompatActivity() {

    private lateinit var progreso: ProgressBar
    private lateinit var estado: TextView
    private lateinit var lista: LinearLayout
    private lateinit var reintentar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barberos)

        progreso = findViewById(R.id.progresoBarberos)
        estado = findViewById(R.id.tvEstadoBarberos)
        lista = findViewById(R.id.listaBarberos)
        reintentar = findViewById(R.id.btnReintentarBarberos)

        findViewById<TextView>(R.id.tvBackBarberos).setOnClickListener {
            finish()
        }

        reintentar.setOnClickListener {
            cargarBarberos()
        }

        cargarBarberos()
    }

    private fun cargarBarberos() {
        progreso.visibility = View.VISIBLE
        estado.visibility = View.VISIBLE
        estado.text = "Cargando barberos..."
        lista.removeAllViews()
        reintentar.visibility = View.GONE

        RetrofitClient.api.listarBarberos()
            .enqueue(object : Callback<List<JsonObject>> {

                override fun onResponse(
                    call: Call<List<JsonObject>>,
                    response: Response<List<JsonObject>>
                ) {
                    progreso.visibility = View.GONE

                    if (response.isSuccessful) {
                        val barberos = response.body().orEmpty()

                        if (barberos.isEmpty()) {
                            estado.text =
                                "Todavía no hay barberos registrados."
                        } else {
                            estado.visibility = View.GONE
                            barberos.forEach { barbero ->
                                agregarTarjeta(barbero)
                            }
                        }
                    } else {
                        mostrarError(
                            "Error del servidor: ${response.code()}"
                        )
                    }
                }

                override fun onFailure(
                    call: Call<List<JsonObject>>,
                    t: Throwable
                ) {
                    progreso.visibility = View.GONE
                    mostrarError(
                        "No se pudo conectar con el servidor."
                    )
                }
            })
    }

    private fun agregarTarjeta(barbero: JsonObject) {
        val nombre = obtenerTexto(
            barbero, "nombre", "nombreBarbero", "nombre_barbero"
        ).ifBlank { "Barbero" }

        val especialidad = obtenerTexto(
            barbero, "especialidad", "descripcion", "cargo"
        ).ifBlank { "Especialista en cortes" }

        val fotoUrl = obtenerTexto(
            barbero, "fotoUrl", "foto_url"
        )

        // Tarjeta horizontal
        val tarjeta = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))

            background = GradientDrawable().apply {
                setColor(Color.rgb(10, 34, 52))
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), Color.rgb(8, 185, 255))
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        // Foto del barbero
        val foto = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                dp(95), dp(95)
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                setColor(Color.rgb(6, 27, 42))
                cornerRadius = dp(12).toFloat()
            }
            clipToOutline = true
            setImageResource(android.R.drawable.ic_menu_camera)
            contentDescription = "Foto de $nombre"
        }

        if (fotoUrl.isNotBlank()) {
            Glide.with(this)
                .load(fotoUrl)
                .placeholder(android.R.drawable.ic_menu_camera)
                .error(android.R.drawable.ic_menu_camera)
                .centerCrop()
                .into(foto)
        }

        // Nombre y especialidad a la derecha
        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, 0, 0)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val titulo = TextView(this).apply {
            text = nombre
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }

        val detalle = TextView(this).apply {
            text = especialidad
            textSize = 14f
            setTextColor(Color.rgb(184, 201, 213))
            setPadding(0, dp(6), 0, 0)
        }

        textos.addView(titulo)
        textos.addView(detalle)

        tarjeta.addView(foto)
        tarjeta.addView(textos)

        lista.addView(tarjeta)
    }

    private fun obtenerTexto(
        objeto: JsonObject,
        vararg claves: String
    ): String {
        for (clave in claves) {
            if (objeto.has(clave) &&
                !objeto.get(clave).isJsonNull
            ) {
                return objeto.get(clave).asString
            }
        }
        return ""
    }

    private fun mostrarError(mensaje: String) {
        estado.visibility = View.VISIBLE
        estado.text = mensaje
        reintentar.visibility = View.VISIBLE

        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun dp(valor: Int): Int {
        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }
}