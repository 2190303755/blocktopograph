package com.mithrilmania.blocktopograph.worldlist

import android.content.Context
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.CardWorldItemBinding
import java.util.*

class WorldItemAdapter(
    val model: WorldListModel
) : RecyclerView.Adapter<WorldItemAdapter.WorldItemHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorldItemHolder {
        val context = parent.context
        return WorldItemHolder(
            context,
            this.model,
            CardWorldItemBinding.inflate(LayoutInflater.from(context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: WorldItemHolder, position: Int) {
        val world = this.model.worlds[position]
        val binding = holder.binding
        holder.world = world
        binding.mode.text = world.mode
        binding.root.setOnClickListener(holder)
        if (world.icon == null) {
            binding.icon.setImageResource(R.drawable.world_icon_default)
        } else {
            binding.icon.setImageBitmap(world.icon)
        }
        binding.name.text = world.name
        binding.date.text =
            DateFormat.getDateFormat(holder.context).format(Date(world.time))
        binding.size.text = world.size ?: holder.context.getString(R.string.calculating_size)
        binding.path.text = world.location.lastPathSegment
        binding.behavior.text = world.behavior.toString()
        binding.resource.text = world.resource.toString()
    }

    class WorldItemHolder(
        val context: Context,
        val model: WorldListModel,
        val binding: CardWorldItemBinding
    ) : RecyclerView.ViewHolder(binding.root), OnClickListener {
        var world: WorldItem? = null
        override fun onClick(v: View?) {
            this.model.selected.value = this.world ?: return
        }
    }

    override fun getItemCount(): Int = this.model.worlds.size
    /*
        private suspend fun getPackAmount(data: ByteArray?): Int {
            return data?.let {
                var amount = 0
                withContext(Default) {
                    try {
                        amount = JSONArray(it.toString(StandardCharsets.UTF_8)).length()
                    } catch (ignored: Exception) {
                    }
                }
                amount
            } ?: 0
        }

        suspend fun addWorld(file: DocumentFile, jobs: ArrayList<Job>, deep: Boolean) {
            if (file.isDirectory) {
                file.findFile(FILE_LEVEL_DAT).let { data ->
                    when {
                        data != null -> {
                            jobs.size.let { index ->
                                jobs.add(index, activity.lifecycleScope.launch(Default) {
                                    val source = async(IO) { activity.readNBTSource(data.uri) }
                                    val bitmap = async(IO) {
                                        file.findFile(FILE_WORLD_ICON)?.let { icon ->
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                activity.contentResolver.loadThumbnail(
                                                    icon.uri, Size(
                                                        activity.resources.getDimension(R.dimen.world_icon_width)
                                                            .toInt(),
                                                        activity.resources.getDimension(R.dimen.world_icon_height)
                                                            .toInt()
                                                    ), null
                                                )
                                            } else activity.contentResolver.openInputStream(icon.uri)
                                                ?.let { BitmapFactory.decodeStream(it) }
                                        }
                                    }
                                    val behavior = async(IO) {
                                        getPackAmount(
                                            file.findFile(FILE_BEHAVIOR_PACKS)
                                                ?.let {
                                                    activity.contentResolver.openInputStream(it.uri)
                                                        ?.readBytes()
                                                }
                                        )
                                    }
                                    val resource = async(IO) {
                                        getPackAmount(
                                            file.findFile(FILE_RESOURCE_PACKS)
                                                ?.let {
                                                    activity.contentResolver.openInputStream(it.uri)
                                                        ?.readBytes()
                                                }
                                        )
                                    }
                                    loadWorld(
                                        jobs,
                                        index,
                                        file,
                                        source.await(),
                                        bitmap.await(),
                                        behavior.await(),
                                        resource.await()
                                    )
                                })
                            }
                        }

                        deep -> {
                            file.listFiles().sortedByDescending { it.lastModified() }
                                .forEach { addWorld(it, jobs, false) }
                        }

                        else -> return
                    }
                }
            }
        }

        private suspend fun loadWorld(
            jobs: ArrayList<Job>,
            index: Int,
            world: DocumentFile,
            source: NBTSource?,
            icon: Bitmap?,
            behavior: Int = 0,
            resource: Int = 0,
        ) {
            withContext(Default) {
                WorldItem(source?.uri, icon, world.lastModified(), behavior, resource).apply {
                    try {
                        path = world.uri.lastPathSegment ?: throw KotlinNullPointerException()
                        source?.tag?.values?.firstOrNull()!!.nbtCompound.let { compound ->
                            compound.getString(WORLD_NAME)?.let { name = it }
                            mode = compound.getGameMode(activity, source.version != null)
                        }
                    } catch (e: Exception) {
                    } finally {
                        jobs.getOrNull(index - 1)?.join()
                        this.uri?.let {
                            worlds.add(this@apply)
                            withContext(Main) {
                                notifyItemInserted(itemCount)
                                activity.setProgressIndeterminate(false)
                            }
                            activity.lifecycleScope.launch(IO) {
                                getWorldSize(world).let { size ->
                                    this@apply.size = size
                                    withContext(Main) {
                                        notifyItemChanged(worlds.indexOf(this@apply))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun loadWorldList(uri: Uri) {
            activity.lifecycleScope.launch(Default) {
                worlds.clear()
                withContext(Main) {
                    notifyDataSetChanged()
                    activity.setProgressIndeterminate(true)
                }
                withContext(IO) {
                    DocumentFile.fromTreeUri(activity, uri)?.let { addWorld(it, arrayListOf(), true) }
                }
            }
        }*/
}