package harmony.valley.wamboocam

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.Html
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.work.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFprobeKit
import com.arthenica.ffmpegkit.MediaInformationSession
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.*
import harmony.valley.wamboocam.databinding.FragmentHomeBinding
import harmony.valley.wamboocam.workers.ForegroundWorker
import harmony.valley.wamboocam.workers.VideoCompressionWorker
import kotlinx.coroutines.*
import java.io.*
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.round
import com.google.android.ump.ConsentInformation.OnConsentInfoUpdateFailureListener
import com.google.android.ump.ConsentInformation.OnConsentInfoUpdateSuccessListener

private const val REQUEST_PICK_VIDEO = 1
private const val REQUEST_PICK_IMAGE = 1

@Suppress("DEPRECATION")
class HomeFragment : Fragment() {
    private lateinit var mAdView: AdView
    private lateinit var logoH : ImageView
    private var videoUrl: Uri? = null
    private var photoUri: Uri? = null
    private var finalPhotoUri: Uri? = null
    private var compressedFilePath = ""
    private var typesSpinner=arrayOf("")
    private var formatsSpinner=arrayOf("")
    private var formatsValues=arrayOf("")
    private var spinner6:Spinner?=null
    private lateinit var pref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var mediaInformation : MediaInformationSession
    private var initialSize = ""
    private var initImageSize = ""
    private var finalImageSize = ""
    private lateinit var videoHeight : String
    private lateinit var  videoWidth : String
    private var imageHeight = ""
    private var  imageWidth = ""
    private var  videoResolution =""
    private var videoResolutionInit = ""
    private var  showSpeed =""
    private var  showCodec =""
    private var  videoCodec =""
    private var  compressSpeed =""
    private lateinit var videoView: VideoView
    private lateinit var videoView2: VideoView
    private lateinit var imageView: ImageView
    private lateinit var imageView2: ImageView
    private var audio = "-c:a copy"
    private lateinit var binding: FragmentHomeBinding
    private var selectedtype = "Ultrafast"
    private var selectedformat = "mp4"
    private var videoformat = "mp4"
    private lateinit var progressDialog: AlertDialog
    private var showViews = true
    private var currentPhotoPath: String = ""
    var  init75 = 0.0
    var init40= 0.0
    var init70= 0.0
    var unidades = ""
    private var imageResolution = ""
    private var isCameraRotated = false // Initialize the rotation flag

    private var selectedVideoUri: Uri? = null
    private lateinit var consentInformation: ConsentInformation
    private lateinit var consentForm: ConsentForm

    //this receiver will trigger when the compression is completed
    private val videoCompressionCompletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Constants.WORK_COMPLETED_ACTION) {
                progressDialog.dismiss()

                Log.d("Service", "Broadcast run")

                if (intent.getStringExtra(RETURN_CODE).equals("0")) { //0 means success
                    val msg1 = getString(R.string.notification_message_success)
                    Toast.makeText(context, msg1, Toast.LENGTH_SHORT).show()
                    AlertDialog.Builder(requireActivity()).apply {
                        val msg2 = getString(R.string.scroll)

                        setMessage(msg2).setPositiveButton(
                            "OK"
                        ) { _, _ -> (requireActivity()) }
                    }.create().show()

                    showDataFromPref()


                } else {
                    val msg1 = getString(R.string.notification_message_failure)
                    Toast.makeText(context, msg1, Toast.LENGTH_SHORT).show()
                    val urlPlay="https://play.google.com/store/apps/details?id=wamboo.example.videocompressor"
                    Toast.makeText(context, getString(R.string.open_wamboo), Toast.LENGTH_SHORT).show()
                    val intent55 = Intent(Intent.ACTION_VIEW, Uri.parse(urlPlay))
                    requireContext().startActivity(intent55)
                }
                if (compressedFilePath != intent.getStringExtra(URI_PATH)){

                    compressedFilePath = intent.getStringExtra(URI_PATH).toString()
                    binding.videoView2.setVideoURI(Uri.parse(compressedFilePath))

                    // after successful retrieval of the video and properly
                    // setting up the retried video uri in
                    // VideoView, Start the VideoView to play that video
                    binding.videoView2.start()
                    binding.videoView2.isVisible=true
                    if (videoResolution == videoResolutionInit) {
                        binding.quality.text =""
                        binding.quality.visibility= View.VISIBLE
                        binding.qualityDescription.visibility= View.VISIBLE
                        binding.checkboxQuality.visibility= View.VISIBLE
                        binding.checkboxQuality.setOnCheckedChangeListener{ _, _ ->
                            val checked: Boolean = binding.checkboxQuality.isChecked
                            if (checked) {
                                /*val snack = Snackbar.make(binding.compressVideo,getString(R.string.waiting),Toast.LENGTH_SHORT)
                                snack.setAnchorView(binding.compressVideo)
                                snack.show()*/



                                calculateQuality()

                            }


                        }
                    } else {
                        binding.quality.visibility= View.GONE
                        binding.qualityDescription.visibility= View.GONE
                        binding.checkboxQuality.visibility= View.GONE
                        binding.quality.text =""
                    }
                }
            }

        }
    }

    private val videoCompressionProgressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action == Constants.WORK_PROGRESS_ACTION) {

                // Do something when the WorkManager completes its work
                // For example, update UI, show a notification, etc.
                if (intent.getStringExtra(RETURN_CODE).equals("0")) { //0 means success
                    val percentage = intent.getStringExtra("percentage")
                    val msg = getString(R.string.waiting)
                    progressDialog.setMessage(msg + "$percentage")
                    if (progressDialog.isShowing.not()) {
                        progressDialog.show()
                    }
                } else {
                    val msg2 = getString(R.string.notification_message_failure)
                    Toast.makeText(context, msg2, Toast.LENGTH_SHORT).show()
                    val urlPlay="https://play.google.com/store/apps/details?id=wamboo.example.videocompressor"
                    Toast.makeText(context, getString(R.string.open_wamboo), Toast.LENGTH_SHORT).show()
                    val intent55 = Intent(Intent.ACTION_VIEW, Uri.parse(urlPlay))
                    requireContext().startActivity(intent55)
                }
            }
        }
    }

    private fun calculateQuality() {
        if (activity != null && videoUrl != null && compressedFilePath != "")  {
        binding.quality.visibility = View.GONE
        binding.qualityDescription.visibility = View.GONE
        binding.checkboxQuality.visibility = View.GONE
        binding.quality.text = ""
        val command2 = "-i ${
            FFmpegKitConfig.getSafParameterForRead(
                activity,
                videoUrl
            )
        } -i ${
            FFmpegKitConfig.getSafParameterForRead(
                activity,
                Uri.parse(compressedFilePath)
            )
        } -lavfi \"ssim;[0:v][1:v]psnr\" -f null -"
        Toast.makeText(
            context,
            Html.fromHtml("<font color='red' ><b>" + getString(R.string.quality_progress) + "</b></font>"),
            Toast.LENGTH_SHORT
        ).show()
        val hola = FFmpegKit.execute(command2)
        binding.quality.visibility = View.VISIBLE
        val indexSsim = hola.logs.size
        val ssimLine = hola.logs[indexSsim - 2]
        val ssim = ssimLine.message.substringAfter("All:").substringBefore("(")
        val quality: Double
        val msg1: String
        if (ssim.contains("0.")) {
            quality = ((1 - ssim.toDouble()) * 100).toBigDecimal().setScale(
                2,
                RoundingMode.UP
            ).toDouble()
            binding.quality.text = buildString {
                append(quality.toString())
                append("%")
            }
            msg1 = getString(R.string.quality_completed) + " " + quality.toString() + "%"
        } else {
            binding.quality.text = getString(R.string.poor_quality)
            msg1 = getString(R.string.poor_quality)
        }
        AlertDialog.Builder(requireActivity()).apply {


            setMessage(msg1).setPositiveButton(
                "OK"
            ) { _, _ -> (requireActivity()) }
        }.create().show()
        binding.quality.visibility = View.VISIBLE
        binding.qualityDescription.visibility = View.VISIBLE
        binding.checkboxQuality.visibility = View.VISIBLE
    } else
        {
            val msg1 = getString(R.string.quality_error)
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:contact.harmonyvalley@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Quality Error")
                putExtra(Intent.EXTRA_TEXT, msg1)
            }

            AlertDialog.Builder(requireActivity()).apply {
                setMessage(msg1)
                setPositiveButton("OK") { _, _ ->
                    // Open email application with "contact.harmonyvalley@gmail.com" in the "To" field
                    try {
                        startActivity(emailIntent)
                    } catch (e: Exception) {
                        // Handle the case when no email application is available or other issues occur
                        // For example, show a toast or a dialog saying that no email app is available
                    }
                }
            }.create().show()
        }
    }
    private fun showStats(
        initialSize2: String?,
        compressedSize: String?,
        conversionTime: String?,
        initialBattery: String?,
        remainingBattery: String?,
        co2: String?,
        showView: Boolean
    ) {
        initialSize = initialSize2!!
        //showing stats data in the textviews
        if (!showView){
            binding.videoView.visibility = View.GONE
            binding.deleteVideo.visibility = View.GONE
            binding.videoView2.visibility = View.GONE
            binding.spinner.visibility = View.GONE
            binding.spinner5.visibility = View.GONE
            binding.spinner2.visibility = View.GONE
            binding.spinner3.visibility = View.GONE
            binding.spinner4.visibility = View.GONE
            binding.checkboxAudio.visibility = View.GONE
            binding.dataTV.visibility=View.GONE
            binding.dataTV2.visibility=View.GONE
            binding.dataTV3.visibility=View.GONE
        }
        when (videoUrl) {
            null -> {
                binding.videoView.visibility = View.GONE
                binding.deleteVideo.visibility = View.GONE
                binding.videoView2.visibility = View.VISIBLE
                binding.videoView2.start()}
            else -> {
                binding.videoView.visibility = View.VISIBLE
                hideInitInfo()
                binding.videoView2.visibility = View.VISIBLE
                binding.deleteVideo.visibility = View.VISIBLE
                binding.videoView.start()
                binding.videoView2.start()
            }
        }

        // now set the video uri in the VideoView


        binding.captureVideo.visibility = View.VISIBLE
        binding.captureImage.isVisible = true
        binding.reset.visibility = View.VISIBLE
        binding.shareVideo.visibility = View.VISIBLE
        binding.statsContainer.visibility = View.VISIBLE
        binding.instructions.isVisible = false
        binding.instructions2.isVisible = false
        binding.instructions3.isVisible = false
        binding.statsContainer2.visibility = View.GONE
        binding.initialSizeTV.text = initialSize
        binding.compressedSizeTV.text = compressedSize
        binding.conversionTimeTV.text = conversionTime
        binding.initialBatteryTV.text = initialBattery
        binding.remainingBatteryTV.text = remainingBattery
        showViews = false


        val pollution= co2!!.toDouble()
        if (pollution > 0) {
            binding.co2TV.setTextColor(Color.parseColor("#FF0000"))
            binding.co2TV.text = co2+ "kgCO2"


        }else{
            binding.co2TV.setTextColor(Color.parseColor("#6F9F3A"))
            binding.co2TV.text = buildString {
                append(co2)
                append("kgCO2")
                append("\n")
                append(getString(R.string.congrats))
            }

        }
        if (compressedSize != null && initialSize != "") {

            val finalSize = compressedSize.substringBefore(" ")
            val finalS = finalSize.replace(",",".").toDouble()
            var final = finalS
            if ((compressedSize.contains("k") && initialSize.contains("M") )||(compressedSize.contains("M") && initialSize.contains("G") ) ||(compressedSize.contains("B") && initialSize.contains("k") )){
                final=finalS/1000
            }
            val initSize = initialSize.substringBefore(" ")
            val init = initSize.replace(",",".")
            val sizeReduction = (100- final.times(100).div(init.toDouble()).toBigDecimal().setScale(2,
                RoundingMode.UP)?.toDouble()!!).toBigDecimal().setScale(2,
                RoundingMode.UP)
            if (sizeReduction<1.toBigDecimal()){
                binding.reduction.text = buildString {
                    append(sizeReduction.toString())
                    append("%")
                    append(" ")
                    append(getString(R.string.noreduction))
                }
            } else {
                binding.reduction.text = buildString {
                    append(sizeReduction.toString())
                    append("%")
                }
            }
        }


    }

    // This is the first method automatically called when the activity starts .
    // Here we initialize all the data which we are going to use and attach the
    // xml layout file with the kotlin code to show the layout and make changes in it when needed
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        savedInstanceState?.let {
            videoUrl = it.getParcelable("videoUrl")
        }
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        pref = requireActivity().getSharedPreferences(
            requireActivity().packageName, Context.MODE_PRIVATE
        )
        editor = pref.edit()

        requireContext().registerReceiver(
            videoCompressionProgressReceiver, IntentFilter(Constants.WORK_PROGRESS_ACTION)
        )
        requireContext().registerReceiver(
            videoCompressionCompletedReceiver, IntentFilter(Constants.WORK_COMPLETED_ACTION)
        )

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {

                    //clearPref()
                    requireActivity().finish()
                }

            })
        logoH =binding.root.findViewById(R.id.bottomImage)
        logoH.setOnClickListener(){
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.data = Uri.parse("https://www.instagram.com/harmonyvalley_official/")
            startActivity(intent)
        }
        initInfo()

        return binding.root
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        // Save state
        outState.putParcelable("videoUrl", videoUrl)
    }
    private fun showDataFromPref() {

        if (pref.getString(RETURN_CODE, "")?.isNotEmpty() == true) {

            showStats(
                pref.getString(INITIAL_SIZE, ""),
                pref.getString(COMPRESS_SZE, ""),
                pref.getString(CONVERSION_TIME, ""),
                pref.getString(INITIAL_BATTERY, ""),
                pref.getString(REMAINING_BATTERY, ""),
                pref.getString(CO2, ""),
                showViews
            )

            //clearPref()

        } else {
            showViews = false
        }
    }

    private fun clearPref() {
        editor.clear().commit()
    }
    private fun loadForm() {
        // Loads a consent form. Must be called on the main thread.
        UserMessagingPlatform.loadConsentForm(
            requireActivity(),
            UserMessagingPlatform.OnConsentFormLoadSuccessListener { form ->
                consentForm = form
                if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    consentForm.show(
                        requireActivity(),
                        ConsentForm.OnConsentFormDismissedListener { dismissal ->
                            if (consentInformation.consentStatus == ConsentInformation.ConsentStatus.OBTAINED) {
                                // App can start requesting ads.
                            }

                            // Handle dismissal by reloading form.
                            loadForm()
                        }
                    )
                }
            },
            UserMessagingPlatform.OnConsentFormLoadFailureListener {
                // Handle the error.
            }
        )
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (view != null) {
            super.onViewCreated(view, savedInstanceState)
            super.onViewCreated(view, savedInstanceState)
            // Set tag for under age of consent. false means users are not under age.
            val params = ConsentRequestParameters.Builder()
                .setTagForUnderAgeOfConsent(false)
                .build()

            consentInformation = UserMessagingPlatform.getConsentInformation(requireContext())
            consentInformation.requestConsentInfoUpdate(
                requireActivity(),
                params,
                OnConsentInfoUpdateSuccessListener {
                    // The consent information state was updated.
                    // You are now ready to check if a form is available.
                    if (consentInformation.isConsentFormAvailable) {
                        // Make sure loadForm() is called in the main thread
                        requireActivity().runOnUiThread {
                            loadForm()
                        }
                    }
                },
                OnConsentInfoUpdateFailureListener {
                    // Handle the error.
                })
        }
        loadAd()
        //checkNotificationPermission()
        checkCameraPermission()
        //checkImagesPermission()
        //checkVideosPermission()
        //checkWritingPermission()
        initUI()

        videoView = binding.root.findViewById(R.id.videoView)
        videoView2 = binding.root.findViewById(R.id.videoView2)
        imageView = binding.root.findViewById(R.id.imageView)
        imageView2 = binding.root.findViewById(R.id.imageView2)
        showLoader()
    }

    private fun checkNotificationPermission() {

        val notificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()

        if (!areNotificationsEnabled) {

            // Create a channel for your notifications
            val channel = NotificationChannel(
                "channel_id", "My Channel", NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager2 =
                requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager2.createNotificationChannel(channel)

            // Ask the user to allow your app to show notifications
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
            startActivity(intent)


        }

    }





    private fun checkCameraPermission() {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Log.i("Permission: ", "Granted")
                } else {
                    Log.i("Permission: ", "Denied")
                }
            }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED -> {


                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA,
                )

            }
        }





    }

    override fun onResume() {
        super.onResume()
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }

        Log.d("service", "OnResume")
        showDataFromPref()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().unregisterReceiver(videoCompressionCompletedReceiver)
        requireActivity().unregisterReceiver(videoCompressionProgressReceiver)
    }

    private fun showLoader() {

        val builder = AlertDialog.Builder(
            requireActivity()
        )
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dialogView = inflater.inflate(R.layout.progress_dialog_layout, null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        progressDialog = builder.create()

    }

    private fun loadAd() {
        MobileAds.initialize(requireActivity()) {}
        val adRequest = AdRequest.Builder().build()
        mAdView = binding.root.findViewById(R.id.adView)
        mAdView.loadAd(adRequest)

    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(requireActivity().packageName)) {

            AlertDialog.Builder(requireActivity()).apply {
                val msg2 = getString(R.string.background2)
                setMessage(msg2).setPositiveButton(
                    getString(R.string.ok)
                ) { _, _ ->  }
            }.create().show()

            return true
        } else {
            showAlertDialog()
            return false

        }

    }

    private fun showAlertDialog() {
        AlertDialog.Builder(requireActivity()).apply {
            val msg1 = getString(R.string.notification_background_title)
            val msg2 = getString(R.string.notification_background_body)
            setTitle(msg1)
            setMessage(msg2).setPositiveButton(
                getString(R.string.ok)
            ) { _, _ -> openBatteryUsagePage(requireActivity()) }
        }.create().show()
    }

    fun openBatteryUsagePage(ctx: Context) {
        val powerUsageIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)


        powerUsageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        powerUsageIntent.data = uri
        try {
            startActivity(powerUsageIntent)
        } catch (e: Exception) {
            Toast.makeText(
                ctx,
                getString(R.string.no_bat_set),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Here we are initialising everything and setting click listeners on the code . Like what will happen
    // When the user tap on pick video button and other buttons
    private fun initUI() = with(binding) {
        reset.setOnClickListener {
            resetViews()
        }


        spinner.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        typesSpinner = arrayOf(
            getString(R.string.select_compression),
            getString(R.string.ultrafast),
            getString(R.string.good),
            getString(R.string.best),
            getString(R.string.custom_h),
            getString(R.string.custom_l)
        )
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.spinner_row, typesSpinner)
        spinner.adapter = arrayAdapter

        spinner5.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        formatsSpinner =
            arrayOf(getString(R.string.select_format), "mp4", "avi", "mov", "mkv", "3gp")
        formatsValues = arrayOf("mp4", "mp4", "avi", "mov", "mkv", "3gp")
        val arrayAdapter2 = ArrayAdapter(requireContext(), R.layout.spinner_list, formatsSpinner)
        spinner5.adapter = arrayAdapter2
        captureVideo.setOnClickListener {


            // if (isBatteryOptimizationDisabled()) {
            resetViews()
            shareVideo.visibility = View.GONE
            deleteVideo.visibility = View.GONE
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    dispatchTakeVideoIntent()

                }

            }


            //  }

        }
        captureImage.setOnClickListener {
            resetViews()
            shareVideo.visibility = View.GONE
            deleteVideo.visibility = View.GONE

            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {

                    dispatchTakeImageIntent()
                }
            }

            when (photoUri) {
                null -> {
                    binding.imageView.visibility = View.GONE
                    Toast.makeText(
                        context,
                        Html.fromHtml("<font color='red' ><b>" + getString(R.string.capture_image) + "</b></font>"),
                        Toast.LENGTH_SHORT
                    ).show()

                }
                else -> {
                    compressImage.setOnClickListener {
                        //clearPref()
                        binding.statsContainer2.visibility = View.GONE
                        if (photoUri != null) {

                            compressImage.isVisible = false

                            saveImageWithDifferentResolution(currentPhotoPath,imageWidth.toInt(),imageHeight.toInt())
                            imageView2.isVisible = true
                            imageView.isVisible = true
                            hideInitInfo()
                            spinner6.isVisible = false
                            binding.shareImage.isVisible = true
                            binding.reset.isVisible = true
                            binding.compressImage.isVisible = false
                            binding.deleteImage.isVisible = true

                        } else {

                            // If picked video is null or video is not picked
                            binding.imageView.visibility = View.GONE

                            binding.imageView2.visibility = View.GONE
                            Toast.makeText(
                                context,
                                Html.fromHtml("<font color='red' ><b>" + getString(R.string.capture_image) + "</b></font>"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }


                    binding.shareImage.setOnClickListener {
                        ShareCompat.IntentBuilder(requireActivity())
                            .setStream(finalPhotoUri)
                            .setType("image/jpeg")
                            .setChooserTitle(getString(R.string.share_compressed_image)).startChooser()
                        //binding.videoView.visibility = View.GONE

                    }

                    binding.deleteImage.setOnClickListener {
                        deleteOriginalImageFromGallery(photoUri)


                    }
                }
            }
        }
        rdOne.setOnClickListener {
            checkNotificationPermission()
            isBatteryOptimizationDisabled()
        }




        when (videoUrl) {
            null -> {
                binding.videoView.visibility = View.GONE

            }
        }// Setting media controller to the video . So the user can pause and play the video . They will appear when user tap on video
        videoView.setMediaController(MediaController(requireActivity()))
        videoView2.setMediaController(MediaController(requireActivity()))
        checkboxAudio.setOnCheckedChangeListener { checkboxAudio, _ ->
            val checked: Boolean = checkboxAudio.isChecked
            if (checked) {
                audio = "-an"
            } else {
                audio = "-c:a copy"
            }
        }
        spinner5.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (view!=null){
                    selectedformat = formatsValues[position]
                    videoformat = formatsSpinner[position]
                    if (videoformat == "3gp" && videoCodec=="libx265")  {
                        videoCodec = "libx264"
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.h265_3gp),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (videoformat == "avi" && videoCodec=="libx265")  {
                        videoCodec = "libx264"
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.h265_avi),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    when (position) {
                        0 -> {
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.no_selected_format),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                requireActivity(),
                                getString(R.string.selected_format) + " " + videoformat,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }

            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }

        /*  with(spinner)
  {setSelection(0, false)}*/
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) { if (view!=null){
                selectedtype = typesSpinner[position]
                val spinner2 = addSpinnerSpeed()
                val spinner4 = addSpinnerCodec()
                val spinner3 = addSpinnerResolution()
                when (selectedtype) {
                    getString(R.string.good) -> {

                        hideSpinner(spinner2)
                        hideSpinner(spinner3)
                        hideSpinner(spinner4)
                        binding.dataTV.visibility = View.VISIBLE
                        binding.dataTV2.visibility = View.VISIBLE
                        binding.dataTV3.visibility = View.VISIBLE
                        binding.dataTV.text = getString(R.string.estimated_size)
                        binding.dataTV2.text = Html.fromHtml(
                            "<b>" + init40.toBigDecimal().setScale(2, RoundingMode.UP)
                                .toDouble() + " $unidades" + "</b>"
                        )
                        binding.dataTV3.text = "40% " + getString(R.string.compression)
                    }
                    getString(R.string.best) -> {

                        hideSpinner(spinner2)
                        hideSpinner(spinner3)
                        hideSpinner(spinner4)
                        binding.dataTV.visibility = View.VISIBLE
                        binding.dataTV2.visibility = View.VISIBLE
                        binding.dataTV3.visibility = View.VISIBLE
                        binding.dataTV.text = getString(R.string.estimated_size)
                        binding.dataTV2.text = Html.fromHtml(
                            "<b>" + init70.toBigDecimal().setScale(2, RoundingMode.UP)
                                .toDouble() + " $unidades" + "</b>"
                        )
                        binding.dataTV3.text = "70% " + getString(R.string.compression)
                    }
                    getString(R.string.ultrafast) -> {

                        hideSpinner(spinner2)
                        hideSpinner(spinner3)
                        hideSpinner(spinner4)
                        binding.dataTV.visibility = View.VISIBLE
                        binding.dataTV2.visibility = View.VISIBLE
                        binding.dataTV3.visibility = View.VISIBLE
                        binding.dataTV.text = getString(R.string.estimated_size)
                        binding.dataTV2.text = Html.fromHtml(
                            "<b>" + init75.toBigDecimal().setScale(2, RoundingMode.UP)
                                .toDouble() + " $unidades" + "</b>"
                        )
                        binding.dataTV3.text = "75% " + getString(R.string.compression)
                    }

                    getString(R.string.custom_h) -> {
                        dataTV.isVisible = false
                        dataTV2.isVisible = false
                        dataTV3.isVisible = false

                    }
                    getString(R.string.custom_l) -> {
                        dataTV.isVisible = false
                        dataTV2.isVisible = false
                        dataTV3.isVisible = false


                    }
                    else -> {

                        hideSpinner(spinner2)
                        hideSpinner(spinner3)
                        hideSpinner(spinner4)
                        binding.dataTV.visibility = View.VISIBLE
                        binding.dataTV2.visibility = View.VISIBLE
                        binding.dataTV3.visibility = View.VISIBLE
                        binding.dataTV.text = getString(R.string.estimated_size)
                        binding.dataTV2.text = Html.fromHtml(
                            "<b>" + init75.toBigDecimal().setScale(2, RoundingMode.UP)
                                .toDouble() + " $unidades" + "</b>"
                        )
                        binding.dataTV3.text = "75% " + getString(R.string.compression)
                    }
                }


            }

            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }
        // Add Spinner to LinearLayout


        //  binding.spinner.visibility=View.VISIBLE







        compressVideo.setOnClickListener {
            //clearPref()
            statsContainer.visibility = View.GONE
            statsContainer2.visibility = View.GONE
            shareVideo.visibility = View.GONE
            deleteVideo.visibility = View.GONE
            binding.checkboxQuality.isChecked = false
            if (videoUrl != null) {

                compressVideo.isVisible = false
                val value =
                    fileSize(videoUrl!!.length(requireActivity().contentResolver))
                editor.putString(INITIAL_SIZE, value)
                editor.commit()
                // When the compress video button is clicked we check if video is already playing then we pause it
                if (videoView.isPlaying) {
                    videoView.pause()
                }
                if (videoView2.isPlaying) {
                    videoView2.pause()
                }

                // Set up the input data for the worker

                val data2 =
                    Data.Builder()
                        .putString(ForegroundWorker.VideoURI, videoUrl?.toString())
                        .putString(ForegroundWorker.SELECTION_TYPE, selectedtype)
                        .putString(ForegroundWorker.SELECTION_FORMAT, selectedformat)
                        .putString(ForegroundWorker.VIDEO_RESOLUTION, videoResolution)
                        .putString(ForegroundWorker.COMPRESS_SPEED, compressSpeed)
                        .putString(ForegroundWorker.VIDEO_CODEC, videoCodec)
                        .putString(ForegroundWorker.VIDEO_AUDIO, audio).build()

                // Create the work request
                val myWorkRequest =
                    OneTimeWorkRequestBuilder<VideoCompressionWorker>().setInputData(data2)
                        .build()

                //initiating WorkManager to start compressing the video
                WorkManager.getInstance(requireContext()).enqueue(myWorkRequest)

            } else {

                // If picked video is null or video is not picked
                binding.videoView.visibility = View.GONE

                binding.videoView2.visibility = View.GONE
                Toast.makeText(
                    context,
                    Html.fromHtml("<font color='red' ><b>" + getString(R.string.capture_video) + "</b></font>"),
                    Toast.LENGTH_SHORT
                ).show()
            }

        }


        binding.shareVideo.setOnClickListener {
            ShareCompat.IntentBuilder(requireActivity())
                .setStream(Uri.parse(compressedFilePath))
                .setType("video/" + selectedformat)
                .setChooserTitle(getString(R.string.share_compressed_video)).startChooser()
            //binding.videoView.visibility = View.GONE

        }

        binding.deleteVideo.setOnClickListener {
            deleteOriginalVideoFromGallery(videoUrl)


        }


    }
    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(requireContext().packageManager)?.also {
                takeVideoLauncher.launch(takeVideoIntent)
            }
        }
    }



    private fun saveImageWithDifferentResolution(imagePath: String, targetWidth: Int, targetHeight: Int) {
        val contentResolver = requireContext().contentResolver
        val imageFileName = "Wamboo_image_${System.currentTimeMillis()}.jpg" // Generate a unique file name

        // Load the original image from file
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)

        // Get the original image dimensions
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // Calculate the aspect ratio of the original image
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

        // Calculate the target aspect ratio
        val targetAspectRatio = targetWidth.toFloat() / targetHeight.toFloat()

        // Determine the resized width and height based on the target aspect ratio
        val resizedWidth: Int
        val resizedHeight: Int

        if (targetAspectRatio > aspectRatio) {
            resizedWidth = targetWidth
            resizedHeight = (targetWidth / aspectRatio).toInt()
        } else {
            resizedWidth = (targetHeight * aspectRatio).toInt()
            resizedHeight = targetHeight
        }

        // Calculate the sample size to resize the image
        val sampleSize = calculateSampleSize(originalWidth, originalHeight, resizedWidth, resizedHeight)

        // Load the resized image using the calculated sample size
        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        val bitmap = BitmapFactory.decodeFile(imagePath, options)

        // Resize the bitmap to match the target dimensions exactly
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, true)

        // Obtain the orientation of the original image from EXIF data
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        // Apply the correct rotation to the resized bitmap based on the EXIF orientation
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        }

        val rotatedResizedBitmap =
            Bitmap.createBitmap(resizedBitmap, 0, 0, resizedWidth, resizedHeight, matrix, true)

        // Create a new bitmap with the desired final resolution
        val finalBitmap = Bitmap.createScaledBitmap(
            rotatedResizedBitmap,
            targetWidth,
            targetHeight,
            true
        )

        // Save the final image
        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }

        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails)
        finalPhotoUri = imageUri
        imageUri?.let { uri ->
            contentResolver.openOutputStream(uri).use { outputStream ->
                outputStream?.let {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream) // Adjust the quality as desired
                }
            }


            imageView2.setImageURI(imageUri)
            imageView2.isVisible = true
            imageView.isVisible = true
            hideInitInfo()
            spinner6?.isVisible = false
            binding.shareImage.isVisible=true
            binding.reset.isVisible=true
            binding.compressImage.isVisible = false
            binding.deleteImage.isVisible = true
        }
        if (finalPhotoUri!=null){
            Toast.makeText(
                context,
                Html.fromHtml("<font color='red' ><b>" + getString(R.string.scroll) + "</b></font>"),
                Toast.LENGTH_SHORT
            ).show()
            if (finalPhotoUri != null) {
                val inputImageFile = File(imagePath)

                initImageSize = getFileSize(inputImageFile)
                finalImageSize = getFileSizeFromUri(requireContext(), imageUri!!)

                val initImageSizeNoUnits = inputImageFile.length()
                val finalImageSizeNoUnits = getFileSizeFromUriNoUnits(requireContext(), imageUri)

                // Calculate the percentage reduction in size
                val reductionPercentage = calculateReductionPercentage(
                    initImageSizeNoUnits.toInt(),
                    finalImageSizeNoUnits.toInt()
                ).toString()

                binding.minPollutionAvoided.setTextColor(Color.parseColor("#6F9F3A"))
                if (!finalImageSize.equals("0.00 KB", ignoreCase = true)) {
                    // Code for non-zero image size
                    binding.statsContainer2.visibility = View.VISIBLE
                    binding.instructions.isVisible = false
                    binding.instructions2.isVisible = false
                    binding.instructions3.isVisible = false
                    binding.initImageSize.text = initImageSize
                    binding.finalImageSize.text = finalImageSize
                    binding.minPollutionAvoided.text = buildString {
                        append(reductionPercentage.toBigDecimal().setScale(2, RoundingMode.UP))
                        append("%")
                        append("\n")
                        append(getString(R.string.congrats))
                    }
                } else {
                    // Code for zero image size
                    binding.statsContainer2.visibility = View.VISIBLE
                    binding.initImageSize.text = initImageSize
                    binding.finalImageSize.text = getString(R.string.size_issue)
                    binding.minPollutionAvoided.text ="90%"
                }

            }


        }
    }
    private fun getFileSizeFromUriNoUnits(context: Context, uri: Uri): Long {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    return it.getLong(sizeIndex)
                }
            }
        }
        return 0L
    }

    private fun getFileSizeFromUri(context: Context, uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    val fileSizeInBytes = it.getLong(sizeIndex)
                    val kiloBytes = fileSizeInBytes / 1024f
                    val megaBytes = kiloBytes / 1024f
                    val fileSizeString = if (megaBytes >= 1) {
                        String.format("%.2f MB", megaBytes)
                    } else {
                        String.format("%.2f KB", kiloBytes)
                    }
                    return fileSizeString.replace(",", ".") // Replace comma from the file size string
                }
            }
        }
        return ""
    }

    // Helper function to calculate the image size in KB or MB

    private fun getFileSize(file: File): String {
        val fileSizeInBytes = file.length()
        val kiloBytes = fileSizeInBytes / 1024f
        val megaBytes = kiloBytes / 1024f
        val fileSizeString = if (megaBytes >= 1) {
            String.format("%.2f MB", megaBytes)
        } else {
            String.format("%.2f KB", kiloBytes)
        }
        return fileSizeString.replace(",", ".") // Replace comma from the file size string
    }

    // Helper function to get the image size in bytes

    private fun calculateReductionPercentage(initialSize: Int, finalSize: Int): Float {
        return if (initialSize > 0) {
            ((initialSize - finalSize) / initialSize.toFloat()) * 100
        } else {
            0f
        }
    }


    private fun calculateSampleSize(imageWidth: Int, imageHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        var sampleSize = 1

        if (imageWidth > targetWidth || imageHeight > targetHeight) {
            val widthRatio = Math.ceil((imageWidth.toFloat() / targetWidth.toFloat()).toDouble()).toInt()
            val heightRatio = Math.ceil((imageHeight.toFloat() / targetHeight.toFloat()).toDouble()).toInt()

            sampleSize = if (heightRatio > widthRatio) heightRatio else widthRatio
        }

        return sampleSize
    }








    private fun dispatchTakeImageIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takeImageIntent ->
            takeImageIntent.resolveActivity(requireContext().packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    ex.printStackTrace()
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    photoUri = FileProvider.getUriForFile(
                        requireContext(),
                        "harmony.valley.wamboocam.provider",
                        it
                    )
                    takeImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    takeImageLauncher.launch(takeImageIntent)
                    isCameraRotated = false // Reset the rotation flag before launching the camera intent
                }
            }
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: throw IOException(getString(R.string.external_directory))
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }
    private fun initInfo() {
        with(binding) {
            instructions.isVisible = true
            instructions2.isVisible = true
            // Add the clickable link in the TextView
            val linkTextView = binding.root.findViewById<TextView>(R.id.instructions2)
            val text = getString(R.string.instructions2)
            val spannableString = SpannableString(text)
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val url = "https://play.google.com/store/apps/details?id=wamboo.example.videocompressor"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

                    try {
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        // Maneja la excepción aquí, por ejemplo, muestra un mensaje de error
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.google_play),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            spannableString.setSpan(
                clickableSpan,
                0,
                text.length,
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            linkTextView.text = spannableString
            linkTextView.movementMethod = LinkMovementMethod.getInstance()
            instructions3.isVisible = true
        }

    }

    private fun hideInitInfo() {
        with(binding) {
            instructions.isVisible = false
            instructions2.isVisible = false
            instructions3.isVisible = false
            // Add the clickable link in the TextView

        } }
    private fun resetViews() {
        with(binding) {

            clearPref()
            captureVideo.isVisible = true
            captureImage.isVisible = true

            spinner.isVisible=false
            spinner6.isVisible=false
            imageView2.isVisible = false
            imageView.isVisible = false
            deleteImage.isVisible = false
            shareImage.isVisible=false
            spinner5.isVisible=false
            spinner2.isVisible=false
            spinner3.isVisible=false
            spinner4.isVisible=false
            statsContainer.isVisible = false
            statsContainer2.isVisible = false
            videoView.isVisible = false
            videoView2.isVisible = false
            checkboxAudio.isVisible=false
            shareVideo.isVisible=false
            deleteVideo.isVisible=false
            compressVideo.isVisible = false
            reset.isVisible = false
            dataTV.isVisible=false
            dataTV2.isVisible=false
            dataTV3.isVisible=false
            rdOne.isChecked=false
            binding.quality.visibility= View.GONE
            binding.qualityDescription.visibility= View.GONE
            binding.checkboxQuality.visibility= View.GONE
            binding.quality.text =""
            spinner.setSelection(0)
            spinner5.setSelection(0)
            spinner6.setSelection(0)
        }
    }


    private fun getImageResolution(photoUri: Uri): Pair<Int, Int>? {
        val inputStream = try {
            requireContext().contentResolver.openInputStream(photoUri)
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)

        try {
            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val imageWidth = options.outWidth
        val imageHeight = options.outHeight

        return if (imageWidth > 0 && imageHeight > 0) {
            Pair(imageWidth, imageHeight)
        } else {
            null
        }
    }

    private fun addSpinnerImageResolution():Spinner {


        binding.spinner6.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        var resolutionSpinner =arrayOf("")
        var resolutionValues =arrayOf("")
        when (photoUri) {
            null -> {




            }
            else -> {
                val resolution = getImageResolution(photoUri!!)
                imageResolution=resolution.toString()
                if (resolution != null) {
                    if (isCameraRotated){
                        imageWidth = resolution.second.toString()
                        imageHeight = resolution.first.toString()
                    }else{
                        imageWidth = resolution.first.toString()
                        imageHeight = resolution.second.toString()
                    }
                    println("Image resolution: $imageWidth x $imageHeight")
                } else {
                    println("Failed to get image resolution.")
                }

                resolutionSpinner = arrayOf(
                    getString(R.string.select_resolution),
                    imageWidth + "x" + imageHeight + "(Original)",
                    "${(round((imageWidth.toDouble() * 0.7)/2)*2).toInt()}" + "x" + "${(round((imageHeight.toDouble() * 0.7)/2)*2).toInt()}" + " (70%)",
                    "${(round((imageWidth.toDouble() * 0.5)/2)*2).toInt()}" + "x" + "${(round((imageHeight.toDouble() * 0.5)/2)*2).toInt()}" + " (50%)",
                    "${(round((imageWidth.toDouble() * 0.25)/2)*2).toInt()}" + "x" + "${(round((imageHeight.toDouble() * 0.25)/2)*2).toInt()}" + " (25%)",
                    "${(round((imageWidth.toDouble() * 0.05)/2)*2).toInt()}" + "x" + "${(round((imageHeight.toDouble() * 0.05)/2)*2).toInt()}" + " (5%)"
                )
                resolutionValues = arrayOf(
                    imageWidth + "x" + imageHeight,
                    imageWidth + "x" + imageHeight,
                    "${(round((imageWidth.toDouble() * 0.7)/2)*2).toInt()}" + "x" + "${(round((imageHeight.toDouble() * 0.7)/2)*2).toInt()}",
                    "${(round((imageWidth.toDouble() * 0.5)/2)*2).toInt()}" + "x" + "${(round((imageHeight.toDouble() * 0.5)/2)*2).toInt()}",
                    "${(round((imageWidth.toDouble() * 0.25)/2)*2).toInt()}" + "x" + "${(round((imageHeight.toDouble() * 0.25)/2)*2).toInt()}",
                    "${(round((imageWidth.toDouble() * 0.05)/2)*2).toInt()}" + "x" + "${(round((imageHeight.toDouble() * 0.05)/2)*2).toInt()}"
                )

            }
        }

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.spinner_list, resolutionSpinner)
        binding.spinner6.adapter = arrayAdapter
        /*with(binding.spinner3)
        {setSelection(0, false)}*/
        binding.spinner6.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) { if (view!=null){
                imageResolution =resolutionValues[position]
                val resolutionParts = imageResolution.split("x")
                imageWidth = resolutionParts.getOrNull(0) ?: ""
                imageHeight = resolutionParts.getOrNull(1) ?: ""
                when (position) {
                    0->{Toast.makeText(
                        requireActivity(),
                        getString(R.string.no_selected_resolution),
                        Toast.LENGTH_SHORT
                    ).show()}
                    else ->{Toast.makeText(
                        requireActivity(),
                        getString(R.string.selected_resolution) + " " + resolutionSpinner[position],
                        Toast.LENGTH_SHORT
                    ).show()
                    }
                }

            }}
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }
        // Add Spinner to LinearLayout

        binding.spinner6.visibility=View.VISIBLE
        return binding.spinner6

    }
    private fun addSpinnerResolution():Spinner {


        binding.spinner3.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        var resolutionSpinner =arrayOf("")
        var resolutionValues =arrayOf("")
        when (videoUrl) {
            null -> {




            }
            else -> {

                resolutionSpinner = arrayOf(
                    getString(R.string.select_resolution),
                    videoWidth + "x" + videoHeight + "(Original)",
                    "${(round((videoWidth.toDouble() * 0.7)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.7)/2)*2).toInt()}" + " (70%)",
                    "${(round((videoWidth.toDouble() * 0.5)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.5)/2)*2).toInt()}" + " (50%)",
                    "${(round((videoWidth.toDouble() * 0.25)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.25)/2)*2).toInt()}" + " (25%)",
                    "${(round((videoWidth.toDouble() * 0.05)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.05)/2)*2).toInt()}" + " (5%)"
                )
                resolutionValues = arrayOf(
                    videoWidth + "x" + videoHeight,
                    videoWidth + "x" + videoHeight,
                    "${(round((videoWidth.toDouble() * 0.7)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.7)/2)*2).toInt()}",
                    "${(round((videoWidth.toDouble() * 0.5)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.5)/2)*2).toInt()}",
                    "${(round((videoWidth.toDouble() * 0.25)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.25)/2)*2).toInt()}",
                    "${(round((videoWidth.toDouble() * 0.05)/2)*2).toInt()}" + "x" + "${(round((videoHeight.toDouble() * 0.05)/2)*2).toInt()}"
                )

            }
        }

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.spinner_list, resolutionSpinner)
        binding.spinner3.adapter = arrayAdapter
        /*with(binding.spinner3)
        {setSelection(0, false)}*/
        binding.spinner3.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) { if (view!=null){
                videoResolution =resolutionValues[position]

                when (position) {
                    0->{Toast.makeText(
                        requireActivity(),
                        getString(R.string.no_selected_resolution),
                        Toast.LENGTH_SHORT
                    ).show()}
                    else ->{Toast.makeText(
                        requireActivity(),
                        getString(R.string.selected_resolution) + " " + resolutionSpinner[position],
                        Toast.LENGTH_SHORT
                    ).show()
                    }
                }

            }}
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }
        // Add Spinner to LinearLayout

        binding.spinner3.visibility=View.VISIBLE
        return binding.spinner3

    }
    private fun addSpinnerSpeed():Spinner {


        binding.spinner2.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        var speedsSpinner = arrayOf("")
        var speedValues = arrayOf("")
        when (videoUrl) {
            null -> {
                binding.videoView.visibility = View.GONE

                Toast.makeText(context, Html.fromHtml("<font color='red' ><b>" +getString(R.string.capture_video)+ "</b></font>"), Toast.LENGTH_SHORT).show()




            }
            else -> {
                compressSpeed ="ultrafast"
                speedsSpinner = arrayOf(getString(R.string.select_speed),getString(R.string.speed1),getString(R.string.speed2),getString(R.string.speed3),getString(R.string.speed4),getString(R.string.speed5),getString(R.string.speed6),getString(R.string.speed7),getString(R.string.speed8),getString(R.string.speed9))
                speedValues = arrayOf("ultrafast","ultrafast", "superfast", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow")

            }
        }

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.spinner_list, speedsSpinner)
        binding.spinner2.adapter = arrayAdapter
        /*with(spinner2)
        {setSelection(0, false)}*/
        binding.spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) { if (view!=null) {
                compressSpeed = speedValues[position]
                showSpeed = speedsSpinner[position]
                when (position) {
                    0 -> {
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.no_selected_speed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            requireActivity(),
                            getString(R.string.selected_speed) + " " + showSpeed,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }
        // Add Spinner to LinearLayout

        binding.spinner2.visibility=View.VISIBLE
        return binding.spinner2

    }

    private fun addSpinnerCodec():Spinner {


        binding.spinner4.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        var codecSpinner = arrayOf("")
        var codecValues = arrayOf("")
        when (videoUrl) {
            null -> {




            }
            else -> {
                videoCodec="libx264"
                codecSpinner = arrayOf(getString(R.string.select_codec),"H.264","H.265")
                codecValues = arrayOf("libx264","libx264","libx265")

            }
        }

        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.spinner_list, codecSpinner)
        binding.spinner4.adapter = arrayAdapter
        /*with(binding.spinner4)
        {setSelection(0, false)}*/
        binding.spinner4.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) { if (view!=null){
                videoCodec =codecValues[position]
                showCodec = codecSpinner[position]
                if (videoformat == "3gp" && videoCodec=="libx265")  {
                    videoCodec = "libx264"
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.h265_3gp),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                if (videoformat == "avi" && videoCodec=="libx265")  {
                    videoCodec = "libx264"
                    Toast.makeText(
                        requireActivity(),
                        getString(R.string.h265_avi),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                when (position) {
                    0->{Toast.makeText(
                        requireActivity(),
                        getString(R.string.no_selected_codec),
                        Toast.LENGTH_SHORT
                    ).show()}
                    else ->{Toast.makeText(
                        requireActivity(),
                        getString(R.string.selected_codec) + " " + showCodec,
                        Toast.LENGTH_SHORT
                    ).show()
                    }
                }

            }}
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Code to perform some action when nothing is selected
            }

        }
        // Add Spinner to LinearLayout
        binding.spinner4.visibility=View.VISIBLE
        return binding.spinner4

    }
    private fun visibleViews() {

        with(binding) {
            captureVideo.isVisible = true
            captureImage.isVisible = true
            //videoView.isVisible = true
            //spinner.isVisible=true
            checkboxAudio.isVisible=true
            compressVideo.isVisible = true
            videoView.visibility = View.GONE
            videoView2.visibility = View.GONE
            spinner.visibility = View.GONE
            spinner5.visibility = View.GONE
            spinner2.visibility = View.GONE
            spinner3.visibility = View.GONE
            spinner4.visibility = View.GONE
            checkboxAudio.visibility = View.GONE
            dataTV.visibility=View.GONE
            dataTV2.visibility=View.GONE
            dataTV3.visibility=View.GONE


        }
    }
    private fun hideSpinner(spinner: Spinner) {
        spinner.visibility= View.GONE


    }


    private fun deleteOriginalVideoFromGallery(videoUri: Uri?) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        startActivityForResult(intent, REQUEST_PICK_VIDEO)
        videoUri?.let { videoUri2 ->
            val deleteIntent = Intent(Intent.ACTION_VIEW)
            deleteIntent.setDataAndType(videoUri2, "video/*")
            deleteIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            deleteIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)

            startActivity(deleteIntent)
        }


        videoUrl = intent.data



    }
    private fun deleteOriginalImageFromGallery(imageUri: Uri?) {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        //startActivityForResult(intent, REQUEST_PICK_IMAGE)
        imageUri?.let { imageUri2 ->
            val deleteIntent = Intent(Intent.ACTION_VIEW)
            deleteIntent.setDataAndType(imageUri2, "image/*")
            deleteIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            deleteIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)

            startActivity(deleteIntent)
        }
        binding.imageView.isVisible= false
        binding.deleteImage.isVisible= false
        photoUri = intent.data
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PICK_VIDEO && resultCode == Activity.RESULT_OK) {
            val videoUri = data?.data
            selectedVideoUri = videoUri
            // Handle the selected video URI

        }
        /*if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)
            imageView.isVisible = true

            spinner6?.isVisible = true
            binding.compressImage.isVisible=true
        }*/
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            photoUri = data?.data
            // Call the delete function or perform any other actions with the selected image URI

        }
    }

    private fun saveImageToGallery(imagePath: String) {
        val contentResolver = requireContext().contentResolver

        val imageFileName = "captured_image_${System.currentTimeMillis()}.jpg" // Generate a unique file name
        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
        }
        // Set the rotation flag based on the camera rotation
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        isCameraRotated = orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270
        val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageDetails)
        photoUri = imageUri
        imageUri?.let { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val inputStream = FileInputStream(imagePath)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.flush()
            }

            imageView.setImageURI(imageUri)
            imageView.isVisible = true
            hideInitInfo()
            spinner6 = addSpinnerImageResolution()
            spinner6?.isVisible = true
            binding.compressImage.isVisible = true
            binding.shareImage.isVisible = false
        }
    }





    private var takeImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->


        if (result.resultCode == Activity.RESULT_OK) {


            // Image captured successfully
            if (currentPhotoPath.isNotEmpty()){
                saveImageToGallery(currentPhotoPath)
                resetViews()
                binding.shareVideo.visibility = View.GONE
                binding.deleteVideo.visibility = View.GONE
                binding.imageView.visibility = View.VISIBLE
                hideInitInfo()
                imageView.setImageURI(currentPhotoPath.toUri())
                imageView.isVisible = true
                hideInitInfo()
                spinner6 = addSpinnerImageResolution()
                spinner6?.isVisible = true
                binding.compressImage.isVisible = true
                binding.shareImage.isVisible = false
            }else
            {

                AlertDialog.Builder(requireActivity()).apply {
                    val msg2 = getString(R.string.rotation_issue)
                    setMessage(msg2).setPositiveButton(
                        getString(R.string.ok)
                    ) { _, _ ->  }
                }.create().show()
            }

        }
    }

    /* This code is using the registerForActivityResult method to launch an activity for a result,
specifically to select a video file. If the result code is Activity.RESULT_OK, it means a video has been successfully selected.
The selected video's Uri is extracted from the Intent returned from the launched activity.
The code then sets the Uri to the VideoView and starts playing the video.
If there is an error in the process, an error message is displayed to the user via a Toast. */
    private var takeVideoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                visibleViews()
                val data: Intent? = result.data
                val videoUri: Uri? = result.data?.data
                videoUri?.let {
                    videoView.setVideoURI(it)
                    videoView.start()
                    binding.spinner.isVisible = true
                    binding.spinner5.isVisible = true

                    binding.checkboxAudio.isVisible=true
                    videoUrl=it

                    //binding.compressVideo.isVisible = true
                }
                if (data != null) {
                    // get the video Uri
                    val uri: Uri? = data.data
                    try {
                        // get the file from the Uri using getFileFromUri() method present
                        // in FileUils.java
                        videoUrl = uri
                        if (videoUrl != null) {

                            mediaInformation = FFprobeKit.getMediaInformation(
                                FFmpegKitConfig.getSafParameterForRead(
                                    activity,
                                    videoUrl
                                )
                            )

                            videoHeight = mediaInformation.mediaInformation.streams[0].height.toString()
                            videoWidth = mediaInformation.mediaInformation.streams[0].width.toString()
                            var side = mediaInformation.mediaInformation.streams[0].getStringProperty("side_data_list")
                            when (videoHeight){
                                "null" ->{
                                    videoHeight = mediaInformation.mediaInformation.streams[1].height.toString()
                                    videoWidth = mediaInformation.mediaInformation.streams[1].width.toString()
                                    side = mediaInformation.mediaInformation.streams[1].getStringProperty("side_data_list")
                                }
                            }
                            //check if video is rotated and swap resolution
                            if (side != null) {
                                val rotation = side.substringAfter("rotation\":").substringBefore('}')
                                if (rotation == "-90" || rotation == "90" || rotation == "-270"){
                                    videoHeight = mediaInformation.mediaInformation.streams[0].width.toString()
                                    videoWidth = mediaInformation.mediaInformation.streams[0].height.toString()
                                    when (videoHeight){
                                        "null" ->{
                                            videoHeight = mediaInformation.mediaInformation.streams[1].width.toString()
                                            videoWidth = mediaInformation.mediaInformation.streams[1].height.toString()
                                        }
                                    }
                                }

                            }

                            videoResolution= videoWidth + "x" + videoHeight
                            videoResolutionInit = videoResolution


                        }
                        binding.videoView.visibility=View.VISIBLE
                        hideInitInfo()
                        // now set the video uri in the VideoView
                        binding.videoView.setVideoURI(uri)

                        // after successful retrieval of the video and properly
                        // setting up the retried video uri in
                        // VideoView, Start the VideoView to play that video
                        binding.videoView.start()
                        initialSize = fileSize(videoUrl!!.length(requireActivity().contentResolver))

                        var initS=0.0
                        if (initialSize != "") {
                            val initSize = initialSize.substringBefore(" ")
                            val init = initSize.replace(",",".").toDouble()

                            if (initialSize.contains("M") )
                            {
                                initS=init*1000000
                            }
                            if (initialSize.contains("G") )
                            {
                                initS=init*1000000000

                            }
                            if (initialSize.contains("k") )
                            {
                                initS=init*1000

                            }

                        }
                        init75=initS*(1-0.75)
                        init40=initS*(1-0.4)
                        init70=initS*(1-0.7)

                        if (initialSize != "") {


                            if (initialSize.contains("M") )
                            {
                                init75 /= 1000000
                                init40 /= 1000000
                                init70 /= 1000000
                                unidades = "MB"
                                if (init75<1){
                                    init75 *= 1000
                                    unidades = "KB"
                                }
                                if (init40<1){
                                    init40 *= 1000
                                    unidades = "KB"
                                }
                                if (init70<1){
                                    init70 *= 1000
                                    unidades = "KB"
                                }
                            }
                            if (initialSize.contains("G") )
                            {
                                init75 /= 1000000000
                                init40 /= 1000000000
                                init70 /= 1000000000
                                unidades = "GB"
                                if (init75<1){
                                    init75 *= 1000
                                    unidades = "MB"
                                }
                                if (init40<1){
                                    init40 *= 1000
                                    unidades = "MB"
                                }
                                if (init70<1){
                                    init70 *= 1000
                                    unidades = "MB"
                                }

                            }
                            if (initialSize.contains("k") )
                            {
                                init75 /= 1000
                                init40 /= 1000
                                init70 /= 1000
                                unidades = "KB"
                                if (init75<1){
                                    init75 *= 1000
                                    unidades = "B"
                                }
                                if (init40<1){
                                    init40 *= 1000
                                    unidades = "B"
                                }
                                if (init70<1){
                                    init70 *= 1000
                                    unidades = "B"
                                }
                            }

                        }
                    } catch (e: Exception) {
                        Toast.makeText(requireActivity(), "Error", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }

            }
        }


    companion object {
        fun newInstance() = HomeFragment()

        const val URI_PATH = "uri_path"
        const val RETURN_CODE = "return_code"
        const val INITIAL_SIZE = "initial_size"
        const val COMPRESS_SZE = "compressed_size"
        const val CONVERSION_TIME = "conversion_time"
        const val INITIAL_BATTERY = "initial_battery"
        const val REMAINING_BATTERY = "remaining_battery"
        const val CO2 = "co2"

    }


}