package pe.edu.cibertec.app_barberia.citas

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.JsonObject
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.data.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReservarCitaActivity : AppCompatActivity() {

    private lateinit var spServicio: Spinner
    private lateinit var spBarbero: Spinner
    private lateinit var spHora: Spinner
    private lateinit var btnFecha: Button
    private lateinit var btnConfirmarCita: Button
    private lateinit var tvEstadoReserva: TextView
    private lateinit var progresoReserva: ProgressBar
    private lateinit var tvBackReservar: TextView

    private val servicios = mutableListOf<JsonObject>()
    private val barberos = mutableListOf<JsonObject>()
    private val calendario = Calendar.getInstance()
    private var fechaSeleccionada = ""
    private var horasSeleccionables = emptyList<String>()

    private var llamadaReserva: Call<JsonObject>? = null
    private var llamadaServicios: Call<List<JsonObject>>? = null
    private var llamadaBarberos: Call<List<JsonObject>>? = null

    private val colorTexto = Color.parseColor("#1D3557")
    private val colorFondoOpciones = Color.parseColor("#F7F9FC")

    private val horariosBase = listOf(
        "09:00", "10:00", "11:00", "12:00", "13:00",
        "14:00", "15:00", "16:00", "17:00", "18:00"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_reservar_cita)
        configurarBarrasSistema()

        spServicio = findViewById(R.id.spServicio)
        spBarbero = findViewById(R.id.spBarbero)
        spHora = findViewById(R.id.spHora)
        btnFecha = findViewById(R.id.btnFecha)
        btnConfirmarCita = findViewById(R.id.btnConfirmarCita)
        tvEstadoReserva = findViewById(R.id.tvEstadoReserva)
        progresoReserva = findViewById(R.id.progresoReserva)
        tvBackReservar = findViewById(R.id.tvBackReservar)

        spHora.adapter = crearAdaptador(
            listOf("Primero selecciona una fecha")
        )

        btnFecha.setOnClickListener {
            seleccionarFecha()
        }

        btnConfirmarCita.setOnClickListener {
            registrarCita()
        }

        tvBackReservar.setOnClickListener {
            finish()
        }

        cargarServicios()
        cargarBarberos()
    }

    override fun onResume() {
        super.onResume()

        if (
            fechaSeleccionada.isNotBlank() &&
            ::spHora.isInitialized
        ) {
            actualizarHorarios()
        }
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

    private fun crearAdaptador(
        opciones: List<String>
    ): ArrayAdapter<String> {
        return object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            opciones
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val vista = super.getView(
                    position,
                    convertView,
                    parent
                ) as TextView

                vista.setTextColor(colorTexto)
                vista.textSize = 14f
                vista.setPadding(
                    dp(12), 0, dp(38), 0
                )
                vista.maxLines = 1
                return vista
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val vista = super.getDropDownView(
                    position,
                    convertView,
                    parent
                ) as TextView

                vista.setTextColor(colorTexto)
                vista.setBackgroundColor(colorFondoOpciones)
                vista.textSize = 14f
                vista.setPadding(
                    dp(18),
                    dp(14),
                    dp(18),
                    dp(14)
                )
                vista.maxLines = 1
                return vista
            }
        }.also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }
    }

    private fun seleccionarFecha() {
        val hoy = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                calendario.set(
                    anio, mes, dia, 0, 0, 0
                )
                calendario.set(
                    Calendar.MILLISECOND, 0
                )

                fechaSeleccionada = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    anio,
                    mes + 1,
                    dia
                )

                btnFecha.text = SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).format(calendario.time)

                actualizarHorarios()
            },
            hoy.get(Calendar.YEAR),
            hoy.get(Calendar.MONTH),
            hoy.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = hoy.timeInMillis
            show()
        }
    }

    private fun actualizarHorarios() {
        val ahora = System.currentTimeMillis()

        horasSeleccionables = horariosBase.filter { hora ->
            val instante = obtenerInstante(
                fechaSeleccionada,
                hora
            )
            instante != null && instante > ahora
        }

        if (horasSeleccionables.isEmpty()) {
            spHora.adapter = crearAdaptador(
                listOf("No quedan horarios para esta fecha")
            )

            tvEstadoReserva.text =
                "Selecciona otro día para reservar tu cita."

            btnConfirmarCita.isEnabled = false
        } else {
            spHora.adapter = crearAdaptador(
                horasSeleccionables
            )

            tvEstadoReserva.text = ""
            btnConfirmarCita.isEnabled = true
        }
    }

    private fun cargarServicios() {
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
                    ) return

                    if (response.isSuccessful) {
                        servicios.clear()
                        servicios.addAll(
                            response.body().orEmpty()
                        )

                        val nombres = servicios.map {
                            it.obtenerTexto(
                                "nombre",
                                "Nombre",
                                "nombreServicio"
                            ) ?: "Servicio"
                        }

                        spServicio.adapter = crearAdaptador(
                            nombres.ifEmpty {
                                listOf("No hay servicios disponibles")
                            }
                        )
                    } else {
                        mostrarMensaje(
                            "No se pudieron cargar los servicios"
                        )
                    }
                }

                override fun onFailure(
                    call: Call<List<JsonObject>>,
                    t: Throwable
                ) {
                    if (
                        !call.isCanceled &&
                        !isFinishing &&
                        !isDestroyed
                    ) {
                        mostrarMensaje(
                            "Error al conectar con los servicios"
                        )
                    }
                }
            }
        )
    }

    private fun cargarBarberos() {
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
                    ) return

                    if (response.isSuccessful) {
                        barberos.clear()
                        barberos.addAll(
                            response.body().orEmpty()
                        )

                        val nombres = barberos.map {
                            it.obtenerTexto(
                                "nombre",
                                "nombreBarbero"
                            ) ?: "Barbero"
                        }

                        spBarbero.adapter = crearAdaptador(
                            nombres.ifEmpty {
                                listOf("No hay barberos disponibles")
                            }
                        )
                    } else {
                        mostrarMensaje(
                            "No se pudieron cargar los barberos"
                        )
                    }
                }

                override fun onFailure(
                    call: Call<List<JsonObject>>,
                    t: Throwable
                ) {
                    if (
                        !call.isCanceled &&
                        !isFinishing &&
                        !isDestroyed
                    ) {
                        mostrarMensaje(
                            "Error al conectar con los barberos"
                        )
                    }
                }
            }
        )
    }

    private fun registrarCita() {
        val idUsuario = getSharedPreferences(
            "BarberApp",
            MODE_PRIVATE
        ).getInt("idUsuario", -1)

        if (idUsuario <= 0) {
            mostrarMensaje("Inicia sesión nuevamente")
            return
        }

        if (fechaSeleccionada.isBlank()) {
            mostrarEstado("Primero selecciona una fecha.")
            return
        }

        if (servicios.isEmpty() || barberos.isEmpty()) {
            mostrarEstado(
                "Espera a que carguen los servicios y barberos."
            )
            return
        }

        if (horasSeleccionables.isEmpty()) {
            mostrarEstado(
                "Selecciona otro día. Ya no quedan horarios futuros."
            )
            return
        }

        val servicio = servicios.getOrNull(
            spServicio.selectedItemPosition
        )

        val barbero = barberos.getOrNull(
            spBarbero.selectedItemPosition
        )

        if (servicio == null || barbero == null) {
            mostrarEstado(
                "Selecciona un servicio y un barbero."
            )
            return
        }

        val idServicio = servicio.obtenerEntero(
            "idServicio", "id_servicio", "id"
        )

        val idBarbero = barbero.obtenerEntero(
            "idBarbero", "id_barbero", "id"
        )

        if (idServicio == null || idBarbero == null) {
            mostrarEstado(
                "No se pudieron identificar el servicio o barbero."
            )
            return
        }

        val hora = spHora.selectedItem
            ?.toString()
            .orEmpty()

        if (hora !in horasSeleccionables) {
            mostrarEstado("Selecciona un horario válido.")
            return
        }

        val fechaHora = obtenerInstante(
            fechaSeleccionada,
            hora
        )

        if (
            fechaHora == null ||
            fechaHora <= System.currentTimeMillis()
        ) {
            actualizarHorarios()
            mostrarEstado(
                "Ese horario ya pasó. Selecciona uno posterior."
            )
            return
        }

        val solicitud = JsonObject().apply {
            addProperty("idUsuario", idUsuario)
            addProperty("idServicio", idServicio)
            addProperty("idBarbero", idBarbero)
            addProperty("fecha", fechaSeleccionada)
            addProperty("hora", hora)
            addProperty("observacion", "")
        }

        progresoReserva.visibility = View.VISIBLE
        btnConfirmarCita.isEnabled = false
        tvEstadoReserva.text = "Procesando reserva..."

        llamadaReserva = RetrofitClient.api.reservarCita(
            solicitud
        )

        llamadaReserva?.enqueue(
            object : Callback<JsonObject> {
                override fun onResponse(
                    call: Call<JsonObject>,
                    response: Response<JsonObject>
                ) {
                    if (
                        isFinishing ||
                        isDestroyed ||
                        call.isCanceled
                    ) return

                    progresoReserva.visibility = View.GONE
                    btnConfirmarCita.isEnabled = true

                    if (response.isSuccessful) {
                        mostrarMensaje(
                            "Reserva registrada correctamente"
                        )
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        mostrarEstado(
                            if (response.code() == 409) {
                                "Ese horario no está disponible. Elige otro e inténtalo nuevamente."
                            } else {
                                "No se pudo registrar la cita (HTTP ${response.code()})."
                            }
                        )
                    }
                }

                override fun onFailure(
                    call: Call<JsonObject>,
                    t: Throwable
                ) {
                    if (
                        isFinishing ||
                        isDestroyed ||
                        call.isCanceled
                    ) return

                    progresoReserva.visibility = View.GONE
                    btnConfirmarCita.isEnabled = true

                    mostrarEstado(
                        "No se pudo conectar con el servidor. Inténtalo nuevamente."
                    )
                }
            }
        )
    }

    private fun obtenerInstante(
        fecha: String,
        hora: String
    ): Long? {
        if (fecha.isBlank() || hora.isBlank()) return null

        return try {
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm",
                Locale.US
            ).apply {
                isLenient = false
            }.parse("$fecha $hora")?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun JsonObject.obtenerTexto(
        vararg claves: String
    ): String? {
        for (clave in claves) {
            val valor = get(clave)

            if (valor != null && !valor.isJsonNull) {
                return try {
                    valor.asString
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    private fun JsonObject.obtenerEntero(
        vararg claves: String
    ): Int? {
        for (clave in claves) {
            val valor = get(clave)

            if (valor != null && !valor.isJsonNull) {
                return try {
                    valor.asInt
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    private fun mostrarEstado(mensaje: String) {
        tvEstadoReserva.text = mensaje
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun mostrarMensaje(mensaje: String) {
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
        llamadaReserva?.cancel()
        llamadaServicios?.cancel()
        llamadaBarberos?.cancel()
        super.onDestroy()
    }
}