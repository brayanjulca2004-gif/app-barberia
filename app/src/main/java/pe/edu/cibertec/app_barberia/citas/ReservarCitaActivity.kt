package pe.edu.cibertec.app_barberia.citas

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    private var fechaSeleccionada = ""
    private val calendario = Calendar.getInstance()

    private val colorTexto = android.graphics.Color.parseColor("#0A2234")
    private val colorFondoOpciones =
        android.graphics.Color.parseColor("#E3EFF8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservar_cita)

        spServicio = findViewById(R.id.spServicio)
        spBarbero = findViewById(R.id.spBarbero)
        spHora = findViewById(R.id.spHora)
        btnFecha = findViewById(R.id.btnFecha)
        btnConfirmarCita = findViewById(R.id.btnConfirmarCita)
        tvEstadoReserva = findViewById(R.id.tvEstadoReserva)
        progresoReserva = findViewById(R.id.progresoReserva)
        tvBackReservar = findViewById(R.id.tvBackReservar)

        configurarHoras()

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

    private fun crearAdaptador(opciones: List<String>): ArrayAdapter<String> {
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
                vista.setPadding(14, 0, 38, 0)
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
                vista.setPadding(20, 14, 20, 14)
                vista.maxLines = 1

                return vista
            }
        }.also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }
    }

    private fun configurarHoras() {
        val horas = listOf(
            "09:00", "10:00", "11:00", "12:00",
            "13:00", "14:00", "15:00", "16:00",
            "17:00", "18:00"
        )

        spHora.adapter = crearAdaptador(horas)
    }

    private fun seleccionarFecha() {
        val hoy = Calendar.getInstance()

        val dialogo = DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                calendario.set(
                    anio, mes, dia, 0, 0, 0
                )
                calendario.set(Calendar.MILLISECOND, 0)

                fechaSeleccionada = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    anio,
                    mes + 1,
                    dia
                )

                val formato = SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

                btnFecha.text = formato.format(calendario.time)
                tvEstadoReserva.text = ""
            },
            hoy.get(Calendar.YEAR),
            hoy.get(Calendar.MONTH),
            hoy.get(Calendar.DAY_OF_MONTH)
        )

        dialogo.datePicker.minDate = hoy.timeInMillis
        dialogo.show()
    }

    private fun cargarServicios() {
        RetrofitClient.api.listarServicios()
            .enqueue(object : Callback<List<JsonObject>> {

                override fun onResponse(
                    call: Call<List<JsonObject>>,
                    response: Response<List<JsonObject>>
                ) {
                    if (response.isSuccessful) {
                        servicios.clear()
                        servicios.addAll(
                            response.body() ?: emptyList()
                        )

                        val nombres = servicios.map {
                            it.obtenerTexto(
                                "nombre",
                                "Nombre",
                                "nombreServicio"
                            ) ?: "Servicio"
                        }

                        spServicio.adapter = crearAdaptador(nombres)
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
                    mostrarMensaje(
                        "Error al conectar con los servicios"
                    )
                }
            })
    }

    private fun cargarBarberos() {
        RetrofitClient.api.listarBarberos()
            .enqueue(object : Callback<List<JsonObject>> {

                override fun onResponse(
                    call: Call<List<JsonObject>>,
                    response: Response<List<JsonObject>>
                ) {
                    if (response.isSuccessful) {
                        barberos.clear()
                        barberos.addAll(
                            response.body() ?: emptyList()
                        )

                        val nombres = barberos.map {
                            it.obtenerTexto(
                                "nombre",
                                "nombreBarbero"
                            ) ?: "Barbero"
                        }

                        spBarbero.adapter = crearAdaptador(nombres)
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
                    mostrarMensaje(
                        "Error al conectar con los barberos"
                    )
                }
            })
    }

    private fun registrarCita() {
        val preferencias = getSharedPreferences(
            "BarberApp",
            MODE_PRIVATE
        )

        val idUsuario = preferencias.getInt("idUsuario", -1)

        if (idUsuario <= 0) {
            mostrarMensaje("Inicia sesión nuevamente")
            return
        }

        if (fechaSeleccionada.isBlank()) {
            mostrarMensaje("Selecciona una fecha")
            return
        }

        if (servicios.isEmpty() || barberos.isEmpty()) {
            mostrarMensaje(
                "Espera a que carguen los servicios y barberos"
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
            mostrarMensaje(
                "Selecciona un servicio y un barbero"
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
            mostrarMensaje(
                "No se pudieron identificar el servicio o barbero"
            )
            return
        }

        val hora = spHora.selectedItem?.toString() ?: ""

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
        tvEstadoReserva.text = ""

        RetrofitClient.api.reservarCita(solicitud)
            .enqueue(object : Callback<JsonObject> {

                override fun onResponse(
                    call: Call<JsonObject>,
                    response: Response<JsonObject>
                ) {
                    progresoReserva.visibility = View.GONE
                    btnConfirmarCita.isEnabled = true

                    if (response.isSuccessful) {
                        tvEstadoReserva.text =
                            "¡Cita reservada correctamente!"

                        mostrarMensaje("Reserva registrada")
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val mensaje = try {
                            response.errorBody()?.string()
                                ?: "No se pudo registrar la cita"
                        } catch (_: Exception) {
                            "No se pudo registrar la cita"
                        }

                        tvEstadoReserva.text = mensaje
                        mostrarMensaje(
                            "No se pudo reservar la cita"
                        )
                    }
                }

                override fun onFailure(
                    call: Call<JsonObject>,
                    t: Throwable
                ) {
                    progresoReserva.visibility = View.GONE
                    btnConfirmarCita.isEnabled = true

                    tvEstadoReserva.text =
                        "No se pudo conectar con el servidor"

                    mostrarMensaje(
                        "Verifica que Spring Boot esté ejecutándose"
                    )
                }
            })
    }

    private fun JsonObject.obtenerTexto(
        vararg claves: String
    ): String? {
        for (clave in claves) {
            if (has(clave) && !get(clave).isJsonNull) {
                return try {
                    get(clave).asString
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
            if (has(clave) && !get(clave).isJsonNull) {
                return try {
                    get(clave).asInt
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(
            this,
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }
}
