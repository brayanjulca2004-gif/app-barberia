package pe.edu.cibertec.app_barberia.barberos

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

class BarberosActivity : AppCompatActivity() {

    private lateinit var progreso: ProgressBar
    private lateinit var estado: TextView
    private lateinit var lista: LinearLayout
    private lateinit var reintentar: Button

    private val azulOscuro = Color.parseColor("#1D3557")
    private val azul = Color.parseColor("#2563A6")
    private val azulSuave = Color.parseColor("#EAF2FB")
    private val textoSecundario = Color.parseColor("#607B9B")
    private val borde = Color.parseColor("#E0E9F4")
    private val blanco = Color.WHITE

    private var llamadaBarberos: Call<List<JsonObject>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_barberos)

        configurarBarrasSistema()

        progreso = findViewById(R.id.progresoBarberos)
        estado = findViewById(R.id.tvEstadoBarberos)
        lista = findViewById(R.id.listaBarberos)
        reintentar = findViewById(R.id.btnReintentarBarberos)

        findViewById<TextView>(
            R.id.tvBackBarberos
        ).setOnClickListener {
            finish()
        }

        reintentar.setOnClickListener {
            cargarBarberos()
        }

        cargarBarberos()
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

    private fun cargarBarberos() {
        llamadaBarberos?.cancel()

        progreso.visibility = View.VISIBLE
        estado.visibility = View.VISIBLE
        estado.text = "Cargando barberos..."

        lista.removeAllViews()
        reintentar.visibility = View.GONE

        llamadaBarberos = RetrofitClient.api.listarBarberos()

        llamadaBarberos?.enqueue(
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
                        val barberos = response.body().orEmpty()

                        if (barberos.isEmpty()) {
                            estado.text =
                                "Todavía no hay barberos registrados."

                            estado.visibility = View.VISIBLE
                        } else {
                            estado.visibility = View.GONE

                            barberos.forEach { barbero ->
                                agregarTarjeta(barbero)
                            }
                        }
                    } else {
                        mostrarError(
                            "No se pudieron cargar los barberos " +
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

    private fun agregarTarjeta(barbero: JsonObject) {
        val nombre = obtenerTexto(
            barbero,
            "nombre",
            "nombreBarbero",
            "nombre_barbero"
        ).ifBlank {
            "Barbero"
        }

        val especialidad = obtenerTexto(
            barbero,
            "especialidad",
            "descripcion",
            "cargo"
        ).ifBlank {
            "Especialista en cortes"
        }

        val fotoUrl = obtenerTexto(
            barbero,
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
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                dp(14),
                dp(14),
                dp(14),
                dp(14)
            )
        }

        val marcoImagen = MaterialCardView(this).apply {
            radius = dp(14).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(azulSuave)
            useCompatPadding = false
            clipToOutline = true

            layoutParams = LinearLayout.LayoutParams(
                dp(98),
                dp(106)
            )
        }

        val foto = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

            scaleType = ImageView.ScaleType.CENTER_CROP

            setImageResource(
                R.drawable.logo_barberapp
            )

            contentDescription = "Foto de $nombre"
        }

        marcoImagen.addView(foto)
        contenido.addView(marcoImagen)

        if (fotoUrl.isNotBlank()) {
            Glide.with(this)
                .load(fotoUrl)
                .placeholder(R.drawable.logo_barberapp)
                .error(R.drawable.logo_barberapp)
                .centerCrop()
                .into(foto)
        }

        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(16)
            }
        }

        val titulo = TextView(this).apply {
            text = nombre
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(azulOscuro)
            maxLines = 3
        }

        textos.addView(titulo)

        val detalle = TextView(this).apply {
            text = especialidad
            textSize = 13f
            setTextColor(textoSecundario)

            setPadding(
                0,
                dp(8),
                0,
                0
            )

            setLineSpacing(
                dp(3).toFloat(),
                1f
            )
        }

        textos.addView(detalle)

        val identificador = TextView(this).apply {
            text = "BARBERAPP"
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setTextColor(azul)
            letterSpacing = 0.08f

            setPadding(
                0,
                dp(13),
                0,
                0
            )
        }

        textos.addView(identificador)

        contenido.addView(textos)
        tarjeta.addView(contenido)
        lista.addView(tarjeta)
    }

    private fun obtenerTexto(
        objeto: JsonObject,
        vararg claves: String
    ): String {
        for (clave in claves) {
            val valor = objeto.get(clave)

            if (
                valor != null &&
                !valor.isJsonNull
            ) {
                return try {
                    valor.asString
                } catch (_: Exception) {
                    ""
                }
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

    override fun onDestroy() {
        llamadaBarberos?.cancel()
        super.onDestroy()
    }
}