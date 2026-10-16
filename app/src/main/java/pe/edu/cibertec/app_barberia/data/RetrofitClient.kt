package pe.edu.cibertec.app_barberia.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL =
       "https://barberapp-king-cxhrbgeef0aah4d7.brazilsouth-01.azurewebsites.net/"
     //"http://192.168.1.49:8080/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}