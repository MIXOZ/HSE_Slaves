package com.hse_project.hse_slaves.activities.pages

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.ViewModelProvider
import com.hse_project.hse_slaves.MainViewModel
import com.hse_project.hse_slaves.MainViewModelFactory
import com.hse_project.hse_slaves.R
import com.hse_project.hse_slaves.activities.MainActivity
import com.hse_project.hse_slaves.current.EVENT_ID
import com.hse_project.hse_slaves.current.IS_TMP_USER
import com.hse_project.hse_slaves.current.TMP_USER_ID
import com.hse_project.hse_slaves.current.USER_ROLE
import com.hse_project.hse_slaves.image.getBitmapByString
import com.hse_project.hse_slaves.model.Event
import com.hse_project.hse_slaves.repository.Repository
import kotlinx.android.synthetic.main.activity_event.*
import kotlinx.android.synthetic.main.activity_event.gallery
import kotlinx.android.synthetic.main.activity_register.*
import retrofit2.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean


class EventActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var data: Event
    private var isLikeSet: Boolean = false
    private var isCheckingLike: AtomicBoolean = AtomicBoolean(false)
    private var likeDelta: Int = 0

    private var isApplied: Boolean = false

    private var isCheckingApply: AtomicBoolean = AtomicBoolean(false)

    private var isAnswering: AtomicBoolean = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event)

        initApi()
        if (USER_ROLE == "CREATOR") {
            checkApply()
            addApplyOnClickListener()
            open_list_of_creators.visibility = GONE

            //TODO проверить что заявка есть и не рассмотрена
            viewModel.checkIfCreatorHasInvitationToEvent(EVENT_ID)
            viewModel.checkIfCreatorHasInvitationToEventResponse.observe(this, {response ->
                if (response.isSuccessful) {
                    viewModel.getInvitation(EVENT_ID)
                    viewModel.getInvitationResponse.observe(this, {response2->
                        if (response2.isSuccessful) {
                            if (!response2.body()!!.accepted){
                                addAcceptDeclineListener()
                            }
                        }
                    })
                }
            })

        } else {
            if (USER_ROLE == "USER") {
                open_list_of_creators.visibility = GONE
            } else {
                addListOfCreatorsListener()
            }
            accept.visibility = GONE
            decline.visibility = GONE
            edit_text_message.visibility = GONE
            send_application.visibility = GONE

            status_image.visibility = GONE
            status_text.visibility = GONE
        }
        addUserOnClickListeners()
        setData()

        addListenerForLikes()
    }

    private fun addListOfCreatorsListener() {
        open_list_of_creators.setOnClickListener {
            //TODO получить список криэйтеров которые подали заявки,стартовать новое активити его надо еще  сделать
            //TODO в активити просто спсисок из названий и аватарок и кнопки принять отклонить
        }
    }

    private fun addAcceptDeclineListener() {
        accept.setOnClickListener {
            if (isAnswering.compareAndSet(false, true)) {
                viewModel.answerInvitationToEvent(EVENT_ID, true)
                viewModel.answerInvitationToEventResponse.observe(this, { response ->
                    if (response.isSuccessful) {
                        accept.visibility = GONE
                        decline.visibility = GONE
                    } else {
                        Log.d("AAA", "BBB")
                        isAnswering.set(false)
                    }
                })
            }
        }
        decline.setOnClickListener {
            if (isAnswering.compareAndSet(false, true)) {
                viewModel.answerInvitationToEvent(EVENT_ID, false)
                viewModel.answerInvitationToEventResponse.observe(this, { response ->
                    if (response.isSuccessful) {
                        accept.visibility = GONE
                        decline.visibility = GONE
                    } else {
                        Log.d("AAA", "BBB")
                        isAnswering.set(false)
                    }
                })
            }
        }
    }

    private fun checkApply() {
        //TODO этот метод точно делает то что надо ??????
        viewModel.checkIfCreatorHasApplicationFromEvent(EVENT_ID)
        viewModel.checkIfCreatorHasApplicationFromEventResponse.observe(this, { response ->
            if (response.isSuccessful) {
                if (response.body() == true) {
                    Log.d("EEEEEEEEEEEEEEEe", "BOOOOOOOYYYYYYYYYYYYYY")
                    //TODO когда появится метод проверить прииняли ли на мероприятие и если да то поменять текст и картинку
                    deleteIfApplied()
                } else {
                    status_image.visibility = GONE
                    status_text.visibility = GONE
                }
            } else {
                Log.d("AAAAAAAAAAAa", "OCHEN' JAL'")
            }
        })
    }

    private fun addApplyOnClickListener() {
        send_application.setOnClickListener {
            Log.d("QQQQQQQQQQ", "1QQQQQQQQQQQQQqq")
            if (isCheckingApply.compareAndSet(false, true)) {
                Log.d("QQQQQQQQQQ", "2QQQQQQQQQQQQQqq")
                if (!isApplied) {
                    Log.d("QQQQQQQQQQ", "3QQQQQQQQQQQQQqq")
                    val message: String =
                        edit_text_message.text.toString().trim { it <= ' ' }
                    viewModel.sendApplicationToEvent(data.id, message)
                    viewModel.sendApplicationToEventResponse.observe(this, { response ->
                        Log.d("QQQQQQQQQQ", "4QQQQQQQQQQQQQqq")
                        if (response.isSuccessful) {
                            Log.d("QQQQQQQQQQ", "5QQQQQQQQQQQQQqq")
                            isApplied = true
                            deleteIfApplied()
                            //onBackPressed()
                        } else {
                            Log.d("AAAAAAAAAA", "OOOOOOOOOOOOOO")
                            Toast.makeText(
                                this@EventActivity,
                                "Something went wrong, try again later",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        isCheckingLike.set(false)
                    })
                }
            }
        }
    }

    private fun deleteIfApplied() {
        edit_text_message.visibility = GONE
        send_application.visibility = GONE

        status_image.visibility = VISIBLE
        status_text.visibility = VISIBLE
    }

    private fun addUserOnClickListeners() {
        organizer_nik.setOnClickListener {
            IS_TMP_USER = true
            startActivity(Intent(this@EventActivity, MainActivity::class.java))
        }
        organizer.setOnClickListener {
            IS_TMP_USER = true
            startActivity(Intent(this@EventActivity, MainActivity::class.java))
        }
    }

    private fun addListenerForLikes() {
        likes_icon.setOnClickListener {
            Log.d("Aaa", "KKKKKKKKKKKKKKKK")
            if (isCheckingLike.compareAndSet(false, true)) {
                if (isLikeSet) {
                    viewModel.deleteLike(data.id)
                    viewModel.deleteLikeResponse.observe(this, { response1 ->
                        if (response1.isSuccessful) {
                            isLikeSet = false
                            changeLikeColor()
                        } else {
                            Log.d("Delete Like", response1.toString())
                        }
                        isCheckingLike.set(false)
                    })
                } else {
                    viewModel.postLike(data.id)
                    viewModel.postLikeResponse.observe(this, { response1 ->
                        if (response1.isSuccessful) {
                            isLikeSet = true
                            changeLikeColor()
                        } else {
                            Log.d("Post Like", response1.toString())
                        }
                        isCheckingLike.set(false)
                    })
                }
            }
        }
    }

    private fun update() {
        val inflater = LayoutInflater.from(this)
        nik_name.text = data.name
        description.text = data.description
        date.text = data.date.toString().substring(0, 10)
        specialization.text = data.specialization
        ratio.text = data.rating.toString()
        geo.text = data.geoData

        changeLikeColor()

        val likesString = data.likes.size.toString() + " likes"
        likes_number.text = likesString

        for (i in data.images.indices) {
            val view = inflater.inflate(R.layout.item_event_photo, gallery, false)

            val imageView: ImageView = view.findViewById<ImageButton>(R.id.imageView)


            imageView.setImageBitmap(getBitmapByString(data.images[i]))
            gallery.addView(view)
        }

    }

    private fun changeLikeColor() {
        var likesString = ""
        likesString = if (isLikeSet) {
            ImageViewCompat.setImageTintList(
                likes_icon,
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.red))
            )
            (data.likes.size + likeDelta).toString() + " likes"
        } else {
            ImageViewCompat.setImageTintList(
                likes_icon,
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.grey))
            )
            (data.likes.size + likeDelta - 1).toString() + " likes"
        }


        likes_number.text = likesString
    }

    fun setData() {
        viewModel.getEvent(EVENT_ID)
        viewModel.eventResponse.observe(this, { response ->
            if (response.isSuccessful) {
                data = response.body()!!
                Log.d("PPPPPPPPPPPP", response.body()?.organizerId.toString())
                Log.d("PPPPPPPPPPPP", response.body()?.name.toString())
                Log.d("OOOOOOOOOOOO", data.organizerId.toString())
                getOrganizer()
                checkLike()
            } else {
                Log.d("AAAAAAAAAAAAAAAAAAAAAAAAAAA", "BBBBB")
            }
        })

    }

    private fun getOrganizer() {
        Log.d("IIIIIIIIIIIIIIIIIII", data.organizerId.toString())
        viewModel.getUser(data.organizerId)
        TMP_USER_ID = data.organizerId
        viewModel.userResponse.observe(this, { response ->
            if (response.isSuccessful) {
                organizer_nik.text = response.body()?.username
            } else {
                Log.d("AAAAAAA", response.toString())
            }
        })
    }

    private fun checkLike() {
        viewModel.checkLike(data.id)
        viewModel.checkLikeResponse.observe(this, { response ->
            if (response.isSuccessful) {
                isLikeSet = response.body()!!
                if (isLikeSet) {
                    likeDelta = 0
                } else {
                    likeDelta = 1
                }
                update()
            } else {
                Log.d("AAAAAAAAAAAAAAAAAAAAAAA", conver(response))
            }
        })
    }

    private fun initApi() {
        val repository = Repository()
        val viewModelFactory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)
    }

    private fun conver(response: Response<Boolean>): String {
        var reader: BufferedReader? = null
        val sb = StringBuilder()
        try {
            reader = BufferedReader(InputStreamReader(response.errorBody()?.byteStream()))
            var line: String?
            try {
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return sb.toString()
    }
}