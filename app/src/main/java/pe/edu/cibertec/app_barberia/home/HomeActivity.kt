package pe.edu.cibertec.app_barberia.home

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import pe.edu.cibertec.app_barberia.R
import pe.edu.cibertec.app_barberia.servicios.ServiciosFragment
import pe.edu.cibertec.app_barberia.barberos.BarberosFragment
import pe.edu.cibertec.app_barberia.citas.CitasFragment
import pe.edu.cibertec.app_barberia.perfil.PerfilFragment

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Cargar InicioFragment la primera vez
        if (savedInstanceState == null) {
            cambiarFragmento(InicioFragment(), "inicio")
        }

        // Configurar clics de la barra inferior
        findViewById<android.view.View>(R.id.navInicio).setOnClickListener {
            cambiarFragmento(InicioFragment(), "inicio")
        }
        findViewById<android.view.View>(R.id.navServicios).setOnClickListener {
            cambiarFragmento(ServiciosFragment(), "servicios")
        }
        findViewById<android.view.View>(R.id.navBarberos).setOnClickListener {
            cambiarFragmento(BarberosFragment(), "barberos")
        }
        findViewById<android.view.View>(R.id.navCitas).setOnClickListener {
            cambiarFragmento(CitasFragment(), "citas")
        }
        findViewById<android.view.View>(R.id.navPerfil).setOnClickListener {
            cambiarFragmento(PerfilFragment(), "perfil")
        }
    }

    fun cambiarFragmento(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        actualizarEstilosBarra(tag)
    }

    private fun actualizarEstilosBarra(tag: String) {
        val colorActivo = 0xFF08B9FF.toInt()
        val colorInactivo = 0xFFAFC6D8.toInt()

        findViewById<TextView>(R.id.iconInicio).setTextColor(if (tag == "inicio") colorActivo else colorInactivo)
        findViewById<TextView>(R.id.textInicio).setTextColor(if (tag == "inicio") colorActivo else colorInactivo)

        findViewById<TextView>(R.id.iconServicios).setTextColor(if (tag == "servicios") colorActivo else colorInactivo)
        findViewById<TextView>(R.id.textServicios).setTextColor(if (tag == "servicios") colorActivo else colorInactivo)

        findViewById<TextView>(R.id.iconBarberos).setTextColor(if (tag == "barberos") colorActivo else colorInactivo)
        findViewById<TextView>(R.id.textBarberos).setTextColor(if (tag == "barberos") colorActivo else colorInactivo)

        findViewById<TextView>(R.id.iconCitas).setTextColor(if (tag == "citas") colorActivo else colorInactivo)
        findViewById<TextView>(R.id.textCitas).setTextColor(if (tag == "citas") colorActivo else colorInactivo)

        findViewById<TextView>(R.id.iconPerfil).setTextColor(if (tag == "perfil") colorActivo else colorInactivo)
        findViewById<TextView>(R.id.textPerfil).setTextColor(if (tag == "perfil") colorActivo else colorInactivo)
    }
}