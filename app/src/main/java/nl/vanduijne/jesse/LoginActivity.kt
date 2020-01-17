package nl.vanduijne.jesse

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import nl.vanduijne.jesse.model.LoginResponse
import nl.vanduijne.jesse.model.RegisterResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    private val service = getService()
    private var isLoginPage = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Back button:
        val toolbar = supportActionBar
        toolbar?.setDisplayHomeAsUpEnabled(true)

        setLoginListener()
        toggleRegisterLogin()
    }

    private fun setRegisterListener(){
        loginButton.setOnClickListener {
            val username = loginField.text.toString()
            val password = passwordField.text.toString()
            val call = service.register(username, password)

            call.enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    if(response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val success = body.Success

                        if(success){
                            login_error_message.text = getString(R.string.msg_account_aangemaakt)
                            switchToLogin()
                            hideKeyboard(login_screen)
                        }
                        else {
                            login_error_message.text = getString(R.string.msg_account_bestaat_al)
                            hideKeyboard(login_screen)
                        }
                    }
                }
                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Log.e("HTTP", "Couldn't fetch a register response")
                }
            })
        }
    }

    private fun setLoginListener(){
        loginButton.setOnClickListener {
            val username = loginField.text.toString()
            val password = passwordField.text.toString()
            val call = service.login(username, password)

            call.enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if(response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val authToken = body.AuthToken

                        val sharedPrefs = getSharedPreferences("nl.vanduijne.jesse", Context.MODE_PRIVATE)
                        val savingSuccess = sharedPrefs.edit().putString(getString(R.string.authTokenKey), authToken).commit()
                        if(savingSuccess) {
                            setDrawerLoginState()
                            finish()

                        }
                        else Log.e("I/O", "Couldn't save the shared preferences")
                    }
                    else {
                        login_error_message.text = getString(R.string.msg_verkeerde_gegevens)
                    }
                }
                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Log.e("HTTP", "Couldn't fetch a login response")
                }
            })
        }
    }

    private fun toggleRegisterLogin(){
        nog_geen_account_button.setOnClickListener {
            loginField.setText("")
            passwordField.setText("")
            login_error_message.text = ""

            if(isLoginPage) {
                switchToRegister()
            }
            else {
                switchToLogin()
            }
        }
    }

    private fun switchToLogin(){
        loginButton.text = getString(R.string.login_button)
        nog_geen_account_button.text = getString(R.string.nog_geen_account)
        setLoginListener()
        isLoginPage = true
    }

    private fun switchToRegister(){
        loginButton.text = getString(R.string.register_button)
        nog_geen_account_button.text = getString(R.string.wel_al_account)
        setRegisterListener()
        isLoginPage = false
    }

    private fun getService(): ArticleService{
        // TODO: Remove duplicate code: use one method which is used here AND in main activity
        val retrofit = Retrofit.Builder()
            .baseUrl("http://inhollandbackend.azurewebsites.net/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ArticleService::class.java)
        return service
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setDrawerLoginState(){
        val favourites = nav_view.menu.findItem(R.id.favourites)
        val logout = nav_view.menu.findItem(R.id.logout)
        val login = nav_view.menu.findItem(R.id.login)

        logout.setVisible(true)
        favourites.setVisible(true)
        login.setVisible(false)
    }

    private fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
