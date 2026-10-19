package pe.edu.cibertec.app_barberia.servicios

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode

class ServiciosActivity : AppCompatActivity() {

    private lateinit var listaServicios: LinearLayout
    private lateinit var progreso: ProgressBar
    private lateinit var tvEstado: TextView
    private lateinit var btnReintentar: Button

    private val azulOscuro = Color.parseColor("#1D3557")
    private val azul = Color.parseColor("#2563A6")
    private val azulSuave = Color.parseColor("#EAF2FB")
    private val textoSecundario = Color.parseColor("#607B9B")
    private val borde = Color.parseColor("#E0E9F4")
    private val blanco = Color.WHITE

    private var llamadaServicios: Call<List<JsonObject>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_servicios)

        configurarBarrasSistema()

        listaServicios = findViewById(R.id.listaServicios)
        progreso = findViewById(R.id.progresoServicios)
        tvEstado = findViewById(R.id.tvEstadoServicios)
        btnReintentar = findViewById(R.id.btnReintentarServicios)

        findViewById<TextView>(
            R.id.tvBackServicios
        ).setOnClickListener {
            finish()
        }

        btnReintentar.setOnClickListener {
            cargarServicios()
        }

        cargarServicios()
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

        val controlador = WindowCompat.getInsetsController(
            window,
            window.decorView
        )

        controlador.isAppearanceLightStatusBars = true
        controlador.isAppearanceLightNavigationBars = true

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
                0,
                0,
                0,
                barras.bottom
            )

            barraInferior.setBackgroundColor(blanco)

            insets
        }
    }

    private fun cargarServicios() {
        llamadaServicios?.cancel()

        progreso.visibility = View.VISIBLE
        tvEstado.visibility = View.VISIBLE
        tvEstado.text = "Cargando servicios..."
        btnReintentar.visibility = View.GONE

        listaServicios.removeAllViews()

        llamadaServicios = RetrofitClient.api.listarServicios()

        llamadaServicios?.enqueue(
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
                    if (
                        call.isCanceled ||
                        isFinishing ||
                        isDestroyed
                    ) {
                        return
                    }

                    progreso.visibility = View.GONE

                    mostrarError(
                        "No se pudo conectar con el servidor."
                    )
                }
            }
        )
    }

    private fun agregarTarjetaServicio(
        servicio: JsonObject
    ) {
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

        val tarjeta = MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(blanco)
            strokeColor = borde
            strokeWidth = dp(1)
            useCompatPadding = false

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        }

        val contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(15),
                dp(15),
                dp(15),
                dp(18)
            )
        }

        val marcoImagen = MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(azulSuave)
            useCompatPadding = false

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(175)
            ).apply {
                bottomMargin = dp(16)
            }
        }

        val imagenServicio = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            scaleType = ImageView.ScaleType.CENTER_CROP

            setImageResource(
                R.drawable.logo_barberapp
            )

            contentDescription = "Fotografía de $nombre"
        }

        marcoImagen.addView(imagenServicio)
        contenido.addView(marcoImagen)

        if (fotoUrl.isNotBlank()) {
            Glide.with(this)
                .load(fotoUrl)
                .placeholder(R.drawable.logo_barberapp)
                .error(R.drawable.logo_barberapp)
                .centerCrop()
                .into(imagenServicio)
        }

        val tvNombre = TextView(this).apply {
            text = nombre
            setTextColor(azulOscuro)
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
        }

        contenido.addView(tvNombre)

        if (descripcion.isNotBlank()) {
            val tvDescripcion = TextView(this).apply {
                text = descripcion
                setTextColor(textoSecundario)
                textSize = 14f

                setLineSpacing(
                    dp(3).toFloat(),
                    1f
                )

                setPadding(
                    0,
                    dp(9),
                    0,
                    dp(3)
                )
            }

            contenido.addView(tvDescripcion)
        }

        if (
            precio.isNotBlank() ||
            duracion.isNotBlank()
        ) {
            val separador = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {
                    topMargin = dp(16)
                    bottomMargin = dp(15)
                }

                setBackgroundColor(borde)
            }

            contenido.addView(separador)

            val filaDetalles = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            if (precio.isNotBlank()) {
                val precioFinal = formatearPrecio(precio)

                val tvPrecio = TextView(this).apply {
                    text = precioFinal
                    setTextColor(azul)
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                    gravity = Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(12),
                        dp(10),
                        dp(12),
                        dp(10)
                    )

                    background = android.graphics.drawable
                        .GradientDrawable().apply {
                            setColor(azulSuave)
                            cornerRadius = dp(11).toFloat()
                        }
                }

                val contenedorPrecio = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL

                    addView(tvPrecio)
                }

                filaDetalles.addView(
                    contenedorPrecio,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                )
            } else {
                filaDetalles.addView(
                    View(this),
                    LinearLayout.LayoutParams(
                        0,
                        1,
                        1f
                    )
                )
            }

            if (duracion.isNotBlank()) {
                val duracionFinal = if (
                    duracion.contains("min", true)
                ) {
                    duracion
                } else {
                    "$duracion min"
                }

                val tvDuracion = TextView(this).apply {
                    text = duracionFinal
                    setTextColor(textoSecundario)
                    textSize = 13f
                    gravity = Gravity.END or Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(8),
                        dp(10),
                        0,
                        dp(10)
                    )
                }

                filaDetalles.addView(
                    tvDuracion,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }

            contenido.addView(filaDetalles)
        }

        tarjeta.addView(contenido)
        listaServicios.addView(tarjeta)
    }

    private fun formatearPrecio(precio: String): String {
        val limpio = precio
            .replace("S/", "", ignoreCase = true)
            .trim()
            .replace(",", ".")

        val numero = limpio.toBigDecimalOrNull()

        return if (numero != null) {
            val formateado = numero
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()

            "S/ $formateado"
        } else {
            if (precio.trim().startsWith("S/", true)) {
                precio.trim()
            } else {
                "S/ ${precio.trim()}"
            }
        }
    }

    private fun obtenerTexto(
        objeto: JsonObject,
        vararg posiblesClaves: String
    ): String {
        for (clave in posiblesClaves) {
            val valor = objeto.get(clave)

            if (
                valor != null &&
                !valor.isJsonNull
            ) {
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

    override fun onDestroy() {
        llamadaServicios?.cancel()
        super.onDestroy()
    }
}