package pe.edu.cibertec.app_barberia.citas

import android.app.AlertDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.gson.JsonObject
import pe.edu.cibertec.app_barberia.R
import java.text.SimpleDateFormat
import java.util.Locale

class CitasAdapter(
    private val citas: MutableList<JsonObject>,
    private val alCancelar: (JsonObject) -> Unit
) : RecyclerView.Adapter<CitasAdapter.CitaViewHolder>() {

    class CitaViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        val tvServicio: TextView = view.findViewById(
            R.id.tvCitaServicio
        )

        val tvBarbero: TextView = view.findViewById(
            R.id.tvCitaBarbero
        )

        val tvFecha: TextView = view.findViewById(
            R.id.tvCitaFecha
        )

        val tvHora: TextView = view.findViewById(
            R.id.tvCitaHora
        )

        val tvEstado: TextView = view.findViewById(
            R.id.tvCitaEstado
        )

        val btnCancelar: Button = view.findViewById(
            R.id.btnCancelarCita
        )
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CitaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_cita,
                parent,
                false
            )

        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CitaViewHolder,
        position: Int
    ) {
        val cita = citas[position]

        fun texto(clave: String): String {
            val valor = cita.get(clave)

            return if (
                valor == null ||
                valor.isJsonNull
            ) {
                ""
            } else {
                valor.asString
            }
        }

        val servicio = texto("nombreServicio")
            .ifBlank { "No disponible" }

        val barbero = texto("nombreBarbero")
            .ifBlank { "No asignado" }

        val estado = texto("estado")
            .ifBlank { "SIN ESTADO" }
            .uppercase(Locale.ROOT)

        holder.tvServicio.text = "Servicio: $servicio"
        holder.tvBarbero.text = "Barbero: $barbero"

        holder.tvFecha.text =
            "Fecha: ${formatearFecha(texto("fecha"))}"

        holder.tvHora.text =
            "Hora: ${formatearHora(texto("hora"))}"

        holder.tvEstado.text = estado

        val colores = when (estado) {
            "PENDIENTE" -> Pair(
                "#FFF4D8", "#986700"
            )

            "CONFIRMADA" -> Pair(
                "#E4F5EA", "#17633B"
            )

            "CANCELADA" -> Pair(
                "#FCE8EB", "#A23B48"
            )

            "FINALIZADA", "COMPLETADA" -> Pair(
                "#EAF2FB", "#2563A6"
            )

            else -> Pair(
                "#EDF2F7", "#52657B"
            )
        }

        (
                holder.tvEstado.parent as? MaterialCardView
                )?.setCardBackgroundColor(
                Color.parseColor(colores.first)
            )

        holder.tvEstado.setTextColor(
            Color.parseColor(colores.second)
        )

        val puedeCancelar =
            estado == "PENDIENTE" ||
                    estado == "CONFIRMADA"

        holder.btnCancelar.visibility =
            if (puedeCancelar) {
                View.VISIBLE
            } else {
                View.GONE
            }

        holder.btnCancelar.setOnClickListener {
            AlertDialog.Builder(
                holder.itemView.context
            )
                .setTitle("Cancelar cita")
                .setMessage(
                    "¿Estás seguro de que deseas cancelar esta cita?"
                )
                .setNegativeButton("No", null)
                .setPositiveButton(
                    "Sí, cancelar"
                ) { _, _ ->
                    alCancelar(cita)
                }
                .show()
        }
    }

    private fun formatearFecha(fecha: String): String {
        if (fecha.isBlank()) return "No disponible"

        return try {
            val formatoEntrada = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).apply {
                isLenient = false
            }

            val fechaParsed = formatoEntrada.parse(
                fecha.substringBefore("T").trim()
            )

            if (fechaParsed == null) {
                fecha
            } else {
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).format(fechaParsed)
            }
        } catch (_: Exception) {
            fecha
        }
    }

    private fun formatearHora(hora: String): String {
        if (hora.isBlank()) return "No disponible"

        return try {
            val formatoEntrada =
                if (hora.trim().length > 5) {
                    "HH:mm:ss"
                } else {
                    "HH:mm"
                }

            val horaParsed = SimpleDateFormat(
                formatoEntrada,
                Locale.US
            ).apply {
                isLenient = false
            }.parse(hora.trim())

            if (horaParsed == null) {
                hora
            } else {
                SimpleDateFormat(
                    "HH:mm",
                    Locale.US
                ).format(horaParsed)
            }
        } catch (_: Exception) {
            hora
        }
    }

    override fun getItemCount(): Int = citas.size

    fun actualizar(nuevasCitas: List<JsonObject>) {
        citas.clear()
        citas.addAll(nuevasCitas)
        notifyDataSetChanged()
    }
}