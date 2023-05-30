package harmony.valley.wamboocam

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import harmony.valley.wamboocam.databinding.FragmentInfoBinding
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan

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

        view = binding.root.findViewById(R.id.planet)
        view.setOnClickListener {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse("https://www.instagram.com/harmonyvalley_official/")
            startActivity(intent)
        }

        // Add the clickable link in the TextView
        val linkTextView = binding.root.findViewById<TextView>(R.id.linkTextView)
        val text = getString(R.string.body5)
        val spannableString = SpannableString(text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = "https://play.google.com/store/apps/details?id=wamboo.example.videocompressor"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
        spannableString.setSpan(clickableSpan, 0, text.length, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        linkTextView.text = spannableString
        linkTextView.movementMethod = LinkMovementMethod.getInstance()

        return binding.root
    }

    companion object {
        fun newInstance() = InfoFragment()
    }
}
