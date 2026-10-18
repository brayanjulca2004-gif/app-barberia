
package pe.edu.cibertec.app_barberia.perfil

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import pe.edu.cibertec.app_barberia.login.MainActivity

class BotonCerrarSesion @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int =
        com.google.android.material.R.attr.materialButtonStyle
) : MaterialButton(context, attrs, defStyleAttr) {

    init {
        setOnClickListener {

            // Borrar los datos de la sesion local
            context.getSharedPreferences(
                "BarberApp",
                Context.MODE_PRIVATE
            )
                .edit()
                .clear()
                .apply()

            // Regresar al inicio de sesion
            val intent = Intent(
                context,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            context.startActivity(intent)
        }
    }
}
