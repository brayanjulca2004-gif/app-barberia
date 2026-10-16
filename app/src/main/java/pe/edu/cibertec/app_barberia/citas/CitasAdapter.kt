
package pe.edu.cibertec.app_barberia.citas

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import pe.edu.cibertec.app_barberia.R

class CitasAdapter(
    private val citas: MutableList<JsonObject>,
    private val alCancelar: (JsonObject) -> Unit
) : RecyclerView.Adapter<CitasAdapter.CitaViewHolder>() {

    class CitaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvServicio: TextView = view.findViewById(R.id.tvCitaServicio)
        val tvBarbero: TextView = view.findViewById(R.id.tvCitaBarbero)
        val tvFecha: TextView = view.findViewById(R.id.tvCitaFecha)
        val tvHora: TextView = view.findViewById(R.id.tvCitaHora)
        val tvEstado: TextView = view.findViewById(R.id.tvCitaEstado)
        val btnCancelar: Button = view.findViewById(R.id.btnCancelarCita)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CitaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cita, parent, false)
        return CitaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CitaViewHolder, position: Int) {
        val cita = citas[position]

        fun texto(clave: String): String {
            val valor = cita.get(clave)
            return if (valor == null || valor.isJsonNull) "No disponible"
            else valor.asString
        }

        holder.tvServicio.text = "Servicio: ${texto("nombreServicio")}"
        holder.tvBarbero.text = "Barbero: ${texto("nombreBarbero")}"
        holder.tvFecha.text = "Fecha: ${texto("fecha")}"
        holder.tvHora.text = "Hora: ${texto("hora")}"
        holder.tvEstado.text = "Estado: ${texto("estado")}"

        val estado = texto("estado")
        val puedeCancelar = estado.equals("PENDIENTE", true) ||
                estado.equals("CONFIRMADA", true)

        holder.btnCancelar.visibility =
            if (puedeCancelar) View.VISIBLE else View.GONE

        holder.btnCancelar.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Cancelar cita")
                .setMessage("¿Estás seguro de que deseas cancelar esta cita?")
                .setNegativeButton("No", null)
                .setPositiveButton("Sí, cancelar") { _, _ ->
                    alCancelar(cita)
                }
                .show()
        }
    }

    override fun getItemCount(): Int = citas.size

    fun actualizar(nuevasCitas: List<JsonObject>) {
        citas.clear()
        citas.addAll(nuevasCitas)
        notifyDataSetChanged()
    }
}