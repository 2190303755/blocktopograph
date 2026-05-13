package com.mithrilmania.blocktopograph.worldlist

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.mithrilmania.blocktopograph.R
import com.mithrilmania.blocktopograph.databinding.CardWorldItemBinding
import com.mithrilmania.blocktopograph.world.WorldDetail
import java.util.Date

class WorldItemAdapter : RecyclerView.Adapter<WorldItemAdapter.WorldItemHolder>() {
    val selected: MutableLiveData<WorldDetail> = MutableLiveData()
    val worlds = SortedList(
        WorldDetail::class.java,
        object : SortedListAdapterCallback<WorldDetail>(this) {
            override fun compare(left: WorldDetail, right: WorldDetail) =
                right.time.compareTo(left.time)

            override fun areContentsTheSame(old: WorldDetail, neo: WorldDetail) =
                old.location == neo.location
                        && old.name == neo.name
                        && old.time == neo.time
                        && old.seed == neo.seed
                        && old.tag == neo.tag
                        && old.mode == neo.mode
                        && old.version == neo.version

            override fun areItemsTheSame(old: WorldDetail, neo: WorldDetail) =
                old.location == neo.location
        }
    )

    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = WorldItemHolder(
        this.selected,
        CardWorldItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: WorldItemHolder, position: Int) {
        val world = this.worlds[position]
        val binding = holder.binding
        val context = holder.binding.root.context
        holder.world = world
        binding.mode.text = world.mode
        binding.root.setOnClickListener(holder)
        if (world.icon == null) {
            binding.icon.setImageResource(R.drawable.world_icon_default)
        } else {
            binding.icon.setImageBitmap(world.icon)
        }
        binding.name.text = world.name
        binding.date.text = DateFormat.getDateFormat(context).format(Date(world.time))
        binding.size.text = world.size ?: context.getString(R.string.calculating_size)
        binding.path.text = world.path
        binding.behavior.text = world.behavior.toString()
        binding.resource.text = world.resource.toString()
    }


    fun notifyItemChanged(world: WorldDetail) {
        val worlds = this.worlds
        val index = worlds.indexOf(world)
        if (index == -1) return
        this.notifyItemChanged(index)
    }

    class WorldItemHolder(
        val selected: MutableLiveData<WorldDetail>,
        val binding: CardWorldItemBinding
    ) : RecyclerView.ViewHolder(binding.root), OnClickListener {
        var world: WorldDetail? = null
        override fun onClick(v: View?) {
            this.selected.value = this.world ?: return
        }
    }

    override fun getItemCount(): Int = this.worlds.size()
}