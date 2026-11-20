package cn.dairo.music.page.accountlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.dairo.music.model.AccountInfo
import cn.dairo.music.store.SettingStore
import cn.dairo.music.util.ObservableObject

class AccountListViewModel : ObservableObject() {

    /** 记录当前要操作的索引 */
    var actionIndex = -1

    /** 当前已登录的账号列表 */
    var accountList by mutableStateOf(emptyList<AccountInfo>())

    /** 获取当前操作的账号 */
    val actionItem: AccountInfo
        get() = this.accountList[this.actionIndex]

    init {
        this.loadData()
    }

    /** 加载数据 */
    fun loadData() {
        this.accountList = SettingStore.accountList
    }

    /** 删除账号点击事件 */
    fun onDeleteClick() {
        val actionItem = this.actionItem
        this.accountList = this.accountList.filter { !it.isSame(actionItem) }
        SettingStore.accountList = this.accountList
    }
}
