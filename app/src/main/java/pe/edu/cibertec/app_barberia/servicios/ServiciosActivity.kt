package pe.edu.cibertec.app_barberia.servicios

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
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

        findViewById<TextView>(R.id.tvBackServicios)
            .setOnClickListener {
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
                    if (isFinishing || isDestroyed) return

                    progreso.visibility = View.GONE

                    if (response.isSuccessful) {

                        val servicios = response.body().orEmpty()

                        if (servicios.isEmpty()) {
                            tvEstado.text =
                                "Todavía no hay servicios disponibles."

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
                    if (isFinishing || isDestroyed) return

                    progreso.visibility = View.GONE

                    mostrarError(
                        "No se pudo conectar con el servidor."
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
        ).ifBlank {
            "Servicio de barbería"
        }

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

        val fotoUrl = obtenerTexto(
            servicio,
            "fotoUrl",
            "foto_url"
        )

        val tarjeta = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(15),
                dp(15),
                dp(15),
                dp(16)
            )

            background = GradientDrawable().apply {
                setColor(Color.rgb(10, 34, 52))
                cornerRadius = dp(18).toFloat()

                setStroke(
                    dp(1),
                    Color.rgb(29, 76, 102)
                )
            }
        }

        val parametros = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(16)
        }

        val imagenServicio = ImageView(this).apply {

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(175)
            ).apply {
                bottomMargin = dp(15)
            }

            scaleType = ImageView.ScaleType.CENTER_CROP

            background = GradientDrawable().apply {
                setColor(Color.rgb(6, 27, 42))
                cornerRadius = dp(13).toFloat()
            }

            clipToOutline = true

            setImageResource(R.drawable.logo_barberapp)

            contentDescription =
                "Fotografía referencial de $nombre"
        }

        if (fotoUrl.isNotBlank()) {

            Glide.with(this)
                .load(fotoUrl)
                .placeholder(R.drawable.logo_barberapp)
                .error(R.drawable.logo_barberapp)
                .centerCrop()
                .into(imagenServicio)
        }

        tarjeta.addView(imagenServicio)

        val tvNombre = TextView(this).apply {

            text = nombre

            setTextColor(Color.WHITE)

            textSize = 19f

            setTypeface(null, Typeface.BOLD)
        }

        tarjeta.addView(tvNombre)

        if (descripcion.isNotBlank()) {

            val tvDescripcion = TextView(this).apply {

                text = descripcion

                setTextColor(
                    Color.rgb(175, 198, 216)
                )

                textSize = 14f

                setPadding(
                    0,
                    dp(9),
                    0,
                    dp(12)
                )
            }

            tarjeta.addView(tvDescripcion)
        }

        val separador = View(this).apply {

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(12)
            }

            setBackgroundColor(
                Color.rgb(29, 76, 102)
            )
        }

        tarjeta.addView(separador)

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

                setTextColor(
                    Color.rgb(8, 185, 255)
                )

                textSize = 14f

                setTypeface(null, Typeface.BOLD)
            }

            tarjeta.addView(tvDetalles)
        }

        listaServicios.addView(
            tarjeta,
            parametros
        )
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

        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }
}