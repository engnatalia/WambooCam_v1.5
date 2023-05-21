package harmony.valley.wamboocam

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import harmony.valley.wamboocam.databinding.FragmentInfoBinding


class InfoFragment : Fragment() {
    private lateinit var mAdView2 : AdView
    private lateinit var view : ImageView

    private lateinit var binding: FragmentInfoBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        MobileAds.initialize(requireActivity()) {}
        val adRequest = AdRequest.Builder().build()
        mAdView2 = binding.root.findViewById(R.id.adView2)
        mAdView2.loadAd(adRequest)
        //return inflater.inflate(R.layout.fragment_info, container, false)
        view =binding.root.findViewById(R.id.planet)
        view.setOnClickListener(){
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse("https://www.instagram.com/harmonyvalley_official/")
            startActivity(intent)
        }
        return binding.root
    }

    companion object {
        fun newInstance() = InfoFragment()
    }
}