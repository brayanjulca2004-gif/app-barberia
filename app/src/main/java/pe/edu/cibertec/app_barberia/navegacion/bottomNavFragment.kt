package pe.edu.cibertec.app_barberia.navegacion

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.home.HomeActivity
import pe.edu.cibertec.app_barberia.servicios.ServiciosActivity
import pe.edu.cibertec.app_barberia.barberos.BarberosActivity
import pe.edu.cibertec.app_barberia.citas.MisCitasActivity
import pe.edu.cibertec.app_barberia.perfil.PerfilActivity

class BottomNavFragment : Fragment(R.layout.fragment_bottom_nav) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.navInicio).setOnClickListener {
            abrirPantalla(HomeActivity::class.java)
        }

        view.findViewById<View>(R.id.navServicios).setOnClickListener {
            abrirPantalla(ServiciosActivity::class.java)
        }

        view.findViewById<View>(R.id.navBarberos).setOnClickListener {
            abrirPantalla(BarberosActivity::class.java)
        }

        view.findViewById<View>(R.id.navCitas).setOnClickListener {
            abrirPantalla(MisCitasActivity::class.java)
        }

        view.findViewById<View>(R.id.navPerfil).setOnClickListener {
            abrirPantalla(PerfilActivity::class.java)
        }
    }

    private fun abrirPantalla(destino: Class<*>) {
        val actividadActual = activity ?: return

        if (destino != actividadActual::class.java) {
            val intent = Intent(actividadActual, destino)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }
    }
}
