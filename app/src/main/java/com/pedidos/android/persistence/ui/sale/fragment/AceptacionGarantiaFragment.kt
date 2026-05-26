package com.pedidos.android.persistence.ui.sale.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.pedidos.android.persistence.databinding.FragmentAceptacionGarantiaBinding

class AceptacionGarantiaFragment : Fragment() {

    private var _binding: FragmentAceptacionGarantiaBinding? = null
    private val binding get() = _binding!!

    // Simulamos la URL que viene de tu API
    private var urlTerminosDesdeApi = "https://www.google.com"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAceptacionGarantiaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Llenar los datos que vienen del servicio (Simulación)
        cargarDatosDelServicio()

        // 2. Click en el texto azul de Política para abrir el Popup
        binding.tvPoliticaLink.setOnClickListener {
            mostrarPopupWeb(urlTerminosDesdeApi)
        }

        // 3. Limpiar firma
        binding.tvLimpiar.setOnClickListener {
            binding.viewFirma.resetCanvasDrawing()
        }

        // 4. Continuar
        binding.btnContinuar.setOnClickListener {
            if (binding.cbPolitica.isChecked) {
                val emailFinal = binding.etEmail.text.toString() // Aquí tomas el email editado
                val firmaBitmap = binding.viewFirma.getSignatureBitmap()
                Toast.makeText(requireContext(), "Email: $emailFinal procesado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Debe aceptar la Política", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Función que simula recibir los datos de tu API
    private fun cargarDatosDelServicio() {
        // Recuperamos los datos del Bundle que enviamos desde la Activity
        val nombreDelServicio = arguments?.getString("NOMBRE_CLIENTE") ?: ""
        val doiDelServicio = arguments?.getString("DOI_CLIENTE") ?: ""
        val emailDelServicio = arguments?.getString("EMAIL_CLIENTE") ?: ""

        // Actualizamos la variable global de la URL
        urlTerminosDesdeApi = arguments?.getString("URL_TERMINOS") ?: "https://www.tuweb.com"

        binding.tvNombreCompleto.text = "Nombre completo: $nombreDelServicio"
        binding.tvDoi.text = "DOI: $doiDelServicio"

        // Seteamos el email, y como es un EditText, el usuario puede borrarlo y escribir otro
        binding.etEmail.setText(emailDelServicio)
    }

    // Función para mostrar la web en un Popup
    private fun mostrarPopupWeb(url: String) {
        // Creamos un WebView dinámicamente
        val webView = WebView(requireContext()).apply {
            webViewClient = WebViewClient() // Para que los links abran dentro del mismo popup
            loadUrl(url)
        }

        // Lo mostramos dentro de un AlertDialog nativo de Android
        AlertDialog.Builder(requireContext())
            .setTitle("Política de Privacidad")
            .setView(webView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}