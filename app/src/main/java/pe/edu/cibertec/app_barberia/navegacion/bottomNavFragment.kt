package pe.edu.cibertec.app_barberia.navegacion

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.barberos.BarberosActivity
import pe.edu.cibertec.app_barberia.citas.MisCitasActivity
import pe.edu.cibertec.app_barberia.citas.ReservarCitaActivity
import pe.edu.cibertec.app_barberia.home.HomeActivity
import pe.edu.cibertec.app_barberia.perfil.PerfilActivity
import pe.edu.cibertec.app_barberia.servicios.ServiciosActivity

class BottomNavFragment : Fragment(R.layout.fragment_bottom_nav) {

    private val azul = Color.parseColor("#2563A6")
    private val gris = Color.parseColor("#899CB0")

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val destinos = listOf<Pair<Int, Class<out Activity>>>(
            R.id.navInicio to HomeActivity::class.java,
            R.id.navServicios to ServiciosActivity::class.java,
            R.id.navBarberos to BarberosActivity::class.java,
            R.id.navCitas to MisCitasActivity::class.java,
            R.id.navPerfil to PerfilActivity::class.java
        )

        destinos.forEach { (id, destino) ->
            view.findViewById<View>(id).setOnClickListener {
                abrirPantalla(destino)
            }
        }

        actualizarSeleccion(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            actualizarSeleccion(it)
        }
    }

    private fun actualizarSeleccion(raiz: View) {
        val pantalla = activity ?: return

        val seleccionado = when (pantalla) {
            is HomeActivity -> R.id.navInicio
            is ServiciosActivity -> R.id.navServicios
            is BarberosActivity -> R.id.navBarberos
            is MisCitasActivity -> R.id.navCitas
            is ReservarCitaActivity -> R.id.navCitas
            is PerfilActivity -> R.id.navPerfil
            else -> R.id.navInicio
        }

        val opciones = listOf(
            Triple(
                R.id.navInicio,
                R.id.iconoInicio,
                R.id.textoInicio
            ),
            Triple(
                R.id.navServicios,
                R.id.iconoServicios,
                R.id.textoServicios
            ),
            Triple(
                R.id.navBarberos,
                R.id.iconoBarberos,
                R.id.textoBarberos
            ),
            Triple(
                R.id.navCitas,
                R.id.iconoCitas,
                R.id.textoCitas
            ),
            Triple(
                R.id.navPerfil,
                R.id.iconoPerfil,
                R.id.textoPerfil
            )
        )

        opciones.forEach { opcion ->
            val activa = opcion.first == seleccionado

            val contenedor = raiz.findViewById<LinearLayout>(
                opcion.first
            )

            val fondo = contenedor.getChildAt(0) as FrameLayout

            val icono = raiz.findViewById<ImageView>(
                opcion.second
            )

            val texto = raiz.findViewById<TextView>(
                opcion.third
            )

            fondo.setBackgroundResource(
                if (activa) {
                    R.drawable.bg_nav_activo
                } else {
                    android.R.color.transparent
                }
            )

            icono.setColorFilter(
                if (activa) azul else gris
            )

            texto.setTextColor(
                if (activa) azul else gris
            )

            texto.setTypeface(
                null,
                if (activa) {
                    Typeface.BOLD
                } else {
                    Typeface.NORMAL
                }
            )
        }
    }

    private fun abrirPantalla(
        destino: Class<out Activity>
    ) {
        val actual = activity ?: return

        if (actual.javaClass == destino) {
            return
        }

        val intent = Intent(
            actual,
            destino
        ).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }

        startActivity(intent)
    }
}