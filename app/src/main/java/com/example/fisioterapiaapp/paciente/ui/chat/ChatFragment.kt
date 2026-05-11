package com.example.fisioterapiaapp.paciente.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fisioterapiaapp.databinding.FragmentChatBinding
import com.example.fisioterapiaapp.paciente.util.gone
import com.example.fisioterapiaapp.paciente.util.visible
import com.example.fisioterapiaapp.paciente.viewmodel.ChatViewModel

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val vm: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarAdapter()
        observarMensajes()

        binding.btnEnviar.setOnClickListener {
            val texto = binding.etMensaje.text.toString().trim()
            if (texto.isEmpty()) return@setOnClickListener
            vm.enviar(texto)
            binding.etMensaje.setText("")
        }
    }

    private fun configurarAdapter() {
        chatAdapter = ChatAdapter()
        binding.rvMensajes.apply {
            layoutManager = LinearLayoutManager(requireContext()).also {
                it.stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun observarMensajes() {
        vm.mensajes.observe(viewLifecycleOwner) { mensajes ->
            chatAdapter.submitList(mensajes) {
                if (mensajes.isNotEmpty()) {
                    binding.rvMensajes.scrollToPosition(mensajes.size - 1)
                }
            }
            if (mensajes.isEmpty()) {
                binding.tvSinMensajes.visible()
            } else {
                binding.tvSinMensajes.gone()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
