package it.hamy.schedule.ui

import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import it.hamy.schedule.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var currentGroup: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val prefs = requireContext().getSharedPreferences("settings", 0)
        currentGroup = prefs.getString("group", "не выбрана") ?: "не выбрана"

        binding.groupText.text = "Текущая группа: $currentGroup"

        binding.changeGroupButton.setOnClickListener {
            prefs.edit().remove("group").apply()
            val intent = Intent(requireContext(), GroupSelectActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }

        binding.refreshButton.setOnClickListener {
            requireActivity().recreate()
        }

        // Устанавливаем текст версии
        setAppVersion()

        // Обработчик клика по кнопке GitHub
        binding.githubButton.setOnClickListener {
            // Открытие GitHub в браузере
            val githubIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/hammsterr/schedule"))
            startActivity(githubIntent)
        }
    }

    private fun setAppVersion() {
        try {
            val pInfo: PackageInfo = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0)
            val version = pInfo.versionName
            binding.versionText.text = "Hamy Studio\nВерсия: $version"
        } catch (e: Exception) {
            e.printStackTrace()
            binding.versionText.text = "Версия: неизвестно"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
