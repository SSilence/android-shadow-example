package de.libexample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import de.libexample.R
import de.libexample.http.BackendService
import de.libexample.http.HttpClient
import de.libexample.http.RestClient

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // http backendService setup
        val httpClient = HttpClient(sslEnabled = false)
        val restClient = RestClient(httpClient, debug = true)
        val backendService = BackendService(restClient)

        // ui setup
        val button = findViewById<Button>(R.id.button)
        val editText = findViewById<TextView>(R.id.editText)
        val textView = findViewById<TextView>(R.id.textView)

        val textView2 = findViewById<TextView>(R.id.textView2)

        button.setOnClickListener {
            backendService.examplePostCall(editText.text.toString()) {
                textView.text = it.OUTPUT
            }
        }

        backendService.exampleGetCall() {
            textView2.text = Json().toJson(it.results)
        }
    }
}