package pe.edu.cibertec.app_barberia.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import pe.edu.cibertec.app_barberia.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ProximasCitasAdapter(
    private val citas: MutableList<JsonObject>
) : RecyclerView.Adapter<ProximasCitasAdapter.CitaViewHolder>() {

    class CitaViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val tvServicio: TextView =
            view.findViewById(R.id.tvServicioProxima)

        val tvBarbero: TextView =
            view.findViewById(R.id.tvBarberoProxima)

        val tvFecha: TextView =
            view.findViewById(R.id.tvFechaProxima)

        val tvRestante: TextView =
            view.findViewById(R.id.tvRestanteProxima)

        val tvEstado: TextView =
            view.findViewById(R.id.tvEstadoProxima)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CitaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_proxima_cita, parent, false)

        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CitaViewHolder,
        position: Int
    ) {
        val cita = citas[position]

        fun texto(clave: String): String {
            val valor = cita.get(clave)

            return if (valor == null || valor.isJsonNull) {
                ""
            } else {
                valor.asString
            }
        }

        holder.tvServicio.text =
            texto("nombreServicio").ifBlank { "Servicio" }

        holder.tvBarbero.text =
            "Barbero: " + texto("nombreBarbero")
                .ifBlank { "No asignado" }

        val estado = texto("estado")
        holder.tvEstado.text = estado

        try {
            val fecha = LocalDate.parse(texto("fecha"))
            val hora = LocalTime.parse(texto("hora"))
            val fechaHora = LocalDateTime.of(fecha, hora)

            val hoy = LocalDate.now()

            val fechaTexto = when (fecha) {
                hoy -> "Hoy"
                hoy.plusDays(1) -> "Mañana"
                else -> fecha.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
                )
            }

            val horaTexto = fechaHora.format(
                DateTimeFormatter.ofPattern("HH:mm")
            )

            holder.tvFecha.text = "$fechaTexto · $horaTexto"

            val minutos = ChronoUnit.MINUTES.between(
                LocalDateTime.now(),
                fechaHora
            )

            holder.tvRestante.text = when {
                minutos <= 0L -> "Tu cita está por comenzar"
                minutos < 60L -> "Faltan $minutos minutos"
                minutos < 1440L -> {
                    val horas = minutos / 60
                    val mins = minutos % 60

                    if (mins == 0L) {
                        "Faltan $horas horas"
                    } else {
                        "Faltan $horas h y $mins min"
                    }
                }
                else -> {
                    val dias = minutos / 1440
                    if (dias == 1L) {
                        "Falta 1 día"
                    } else {
                        "Faltan $dias días"
                    }
                }
            }
        } catch (_: Exception) {
            holder.tvFecha.text =
                "${texto("fecha")} · ${texto("hora")}"
            holder.tvRestante.text = ""
        }
    }

    override fun getItemCount(): Int = citas.size

    fun actualizar(nuevasCitas: List<JsonObject>) {
        citas.clear()
        citas.addAll(nuevasCitas)
        notifyDataSetChanged()
    }
}
