<!--
 * @Descripttion: 璐墿杞? * @version: V 1.0
 * @Author: twx
 * @Date: 2020-05-14 13:38:52
 -->
<template>
  <div class="shoppingCart">
    <div class="cart-header">
      <div class="cart-header-content">
        <p>
          <i class="el-icon-shopping-cart-full" style="color:#ff6700; font-weight: 600;"></i>
          我的购物车
        </p>
        <span>温馨提示：商品是否购买成功，以最终下单为准，请尽快结算</span>
      </div>
    </div>

    <div class="content" v-if="carts.length > 0">
      <ul>
        <li class="header">
          <div class="pro-check">
            <el-checkbox v-model="isAllCheck">全选</el-checkbox>
          </div>
          <div class="pro-img"></div>
          <div class="pro-name">商品名称</div>
          <div class="pro-price">单价</div>
          <div class="pro-num">数量</div>
          <div class="pro-total">小计</div>
          <div class="pro-action">操作</div>
        </li>

        <li class="product-list" v-for="(item,index) in carts" :key="item.skuId || index">
          <div class="pro-check">
            <el-checkbox :value="true" @change="checkChange($event,index)"></el-checkbox>
          </div>
          <div class="pro-img">
            <router-link :to="{ path: '/goods/details', query: { productID: item.skuId } }">
              <img :src="$imageUrl(item.image)" @error="$imageError" />
            </router-link>
          </div>
          <div class="pro-name">
            <router-link :to="{ path: '/goods/details', query: { productID: item.skuId } }">
              {{ item.title && item.title.length > 21 ? item.title.substring(0, 21) : item.title }}
            </router-link>
          </div>
          <div class="pro-price">{{ priceYuan(item.price) }}元</div>
          <div class="pro-num">
            <el-input-number
              size="small"
              :value="item.num"
              @change="handleChange($event,index,item.skuId)"
              :min="1"
              :max="999"
            ></el-input-number>
          </div>
          <div class="pro-total pro-total-in">{{ priceYuan(item.price * item.num) }}元</div>
          <div class="pro-action">
            <el-popover placement="right">
              <p>确定删除吗？</p>
              <div style="text-align: right; margin: 10px 0 0">
                <el-button type="primary" size="mini" @click="deleteItem($event,index,item.skuId)">确定</el-button>
              </div>
              <i class="el-icon-error" slot="reference" style="font-size: 18px;"></i>
            </el-popover>
          </div>
        </li>
      </ul>
      <div style="height:20px;background-color: #f5f5f5"></div>

      <div class="cart-bar">
        <div class="cart-bar-left">
          <span>
            <router-link to="/goods">继续购物</router-link>
          </span>
          <span class="sep">|</span>
          <span class="cart-total">
            共 <span class="cart-total-num">{{ carts.length }}</span> 件商品，已选择
            <span class="cart-total-num">{{ mycount }}</span> 件
          </span>
        </div>
        <div class="cart-bar-right">
          <span>
            <span class="total-price-title">合计：</span>
            <span class="total-price">{{ priceYuan(totalPrice) }}元</span>
          </span>
          <router-link :to="mycount > 0 ? '/confirmOrder' : ''">
            <div :class="mycount > 0 ? 'btn-primary' : 'btn-primary-disabled'">去结算</div>
          </router-link>
        </div>
      </div>
    </div>

    <div v-else class="cart-empty">
      <div class="empty">
        <h2>您的购物车还是空的！</h2>
        <p>快去购物吧！</p>
      </div>
    </div>
  </div>
</template>
<script>
import { mapActions } from "vuex";
import { mapGetters } from "vuex";

export default {
  data() {
    return {
        carts:[],
        mycount:"",
        totalPrice:""
    };
  },
  mounted(){
      this.getCarts();
  },
  methods: {
    ...mapActions(["updateShoppingCart", "deleteShoppingCart", "checkAll"]),
    // 淇敼鍟嗗搧鏁伴噺鐨勬椂鍊欒皟鐢ㄨ鍑芥暟
    handleChange(currentValue, key, productID) {
      this.updateShoppingCart({ key: key, prop: "check", val: true });
      this.$http.put("/cart", {
          userId: this.$store.getters.getUser.user_id,
          skuId: productID,
          num: currentValue
        })
        .then(res => {
          if (res.status === 204 || res.status === 200) {
            this.updateShoppingCart({
              key: key,
              prop: "num",
              val: currentValue
            });
            this.getCarts();
            this.notifySucceed("购物车更新成功");
            return;
          }
          this.notifyError("购物车更新失败");
        })
        .catch(err => {
          this.notifyError("购物车更新失败");
          return Promise.reject(err);
        });
    },
    checkChange(val, key) {
      this.updateShoppingCart({ key: key, prop: "check", val: val });
    },
    // 鍒犻櫎璐墿杞﹀唴鍟嗗搧
    deleteItem(e, id, productID) {
      this.$http.delete("/cart/"+productID)
        .then(res => {
            if(res.status == 200){
              this.deleteShoppingCart(id);
              this.getCarts();
              // 鎻愮ず鍒犻櫎鎴愬姛淇℃伅
              this.notifySucceed("鍒犻櫎鎴愬姛");
            }else{
                 // 鎻愮ず鍒犻櫎澶辫触淇℃伅
              this.notifyError(res.data.msg);
          }
         }).catch(err => {
          return Promise.reject(err);
        });
    },

    getCarts(){
      this.$http.get("/cart/find").then(res => {
        if (res.status == 200) {
          this.carts = Array.isArray(res.data) ? res.data : [];
          let count = 0;
          let totalPrice = 0;
          for(let n = 0; n < this.carts.length; n++){
            count = count + this.carts[n].num;
            totalPrice = totalPrice + this.carts[n].num * this.carts[n].price;
          }
          this.mycount = count;
          this.totalPrice = totalPrice;
        } else {
          this.notifyError("系统繁忙，请稍后再试");
        }
      }).catch(err => {
        this.notifyError("购物车加载失败");
        return Promise.reject(err);
      });
    },
    priceYuan(value) {
      return Number(value || 0).toFixed(2).replace(/\.00$/, "");
    }
  },
  computed: {
    ...mapGetters([
      "getShoppingCart",
      "getCheckNum",
      "getTotalPrice",
      "getNum"
    ]),
    isAllCheck: {
      get() {
        return this.$store.getters.getIsAllCheck;
      },
      set(val) {
        this.checkAll(val);
      }
    }
  }
};
</script>
<style scoped>
.shoppingCart {
  background-color: #f5f5f5;
  padding-bottom: 20px;
}
/* 璐墿杞﹀ご閮–SS */
.shoppingCart .cart-header {
  height: 64px;
  border-bottom: 2px solid #ff6700;
  background-color: #fff;
  margin-bottom: 20px;
}
.shoppingCart .cart-header .cart-header-content {
  width: 1225px;
  margin: 0 auto;
}
.shoppingCart .cart-header p {
  font-size: 28px;
  line-height: 58px;
  float: left;
  font-weight: normal;
  color: #424242;
}
.shoppingCart .cart-header span {
  color: #757575;
  font-size: 12px;
  float: left;
  height: 20px;
  line-height: 20px;
  margin-top: 30px;
  margin-left: 15px;
}
/* 璐墿杞﹀ご閮–SS END */

/* 璐墿杞︿富瑕佸唴瀹瑰尯CSS */
.shoppingCart .content {
  width: 1225px;
  margin: 0 auto;
  background-color: #fff;
}

.shoppingCart .content ul {
  background-color: #fff;
  color: #424242;
  line-height: 85px;
}
/* 璐墿杞﹁〃澶村強CSS */
.shoppingCart .content ul .header {
  height: 85px;
  padding-right: 26px;
  color: #424242;
}
.shoppingCart .content ul .product-list {
  height: 85px;
  padding: 15px 26px 15px 0;
  border-top: 1px solid #e0e0e0;
}
.shoppingCart .content ul .pro-check {
  float: left;
  height: 85px;
  width: 110px;
}
.shoppingCart .content ul .pro-check .el-checkbox {
  font-size: 16px;
  margin-left: 24px;
}
.shoppingCart .content ul .pro-img {
  float: left;
  height: 85px;
  width: 120px;
}
.shoppingCart .content ul .pro-img img {
  height: 80px;
  width: 80px;
}
.shoppingCart .content ul .pro-name {
  float: left;
  width: 380px;
}
.shoppingCart .content ul .pro-name a {
  color: #424242;
}
.shoppingCart .content ul .pro-name a:hover {
  color: #ff6700;
}
.shoppingCart .content ul .pro-price {
  float: left;
  width: 140px;
  padding-right: 18px;
  text-align: center;
}
.shoppingCart .content ul .pro-num {
  float: left;
  width: 150px;
  text-align: center;
}
.shoppingCart .content ul .pro-total {
  float: left;
  width: 120px;
  padding-right: 81px;
  text-align: right;
}
.shoppingCart .content ul .pro-total-in {
  color: #ff6700;
}
.shoppingCart .content ul .pro-action {
  float: left;
  width: 80px;
  text-align: center;
}
.shoppingCart .content ul .pro-action i:hover {
  color: #ff6700;
}
/* 璐墿杞﹁〃澶村強CSS END */

/* 璐墿杞﹀簳閮ㄥ鑸潯CSS */
.shoppingCart .cart-bar {
  width: 1225px;
  height: 50px;
  line-height: 50px;
  background-color: #fff;
}
.shoppingCart .cart-bar .cart-bar-left {
  float: left;
}
.shoppingCart .cart-bar .cart-bar-left a {
  line-height: 50px;
  margin-left: 32px;
  color: #757575;
}
.shoppingCart .cart-bar .cart-bar-left a:hover {
  color: #ff6700;
}
.shoppingCart .cart-bar .cart-bar-left .sep {
  color: #eee;
  margin: 0 20px;
}
.shoppingCart .cart-bar .cart-bar-left .cart-total {
  color: #757575;
}
.shoppingCart .cart-bar .cart-bar-left .cart-total-num {
  color: #ff6700;
}
.shoppingCart .cart-bar .cart-bar-right {
  float: right;
}
.shoppingCart .cart-bar .cart-bar-right .total-price-title {
  color: #ff6700;
  font-size: 14px;
}
.shoppingCart .cart-bar .cart-bar-right .total-price {
  color: #ff6700;
  font-size: 30px;
}
.shoppingCart .cart-bar .cart-bar-right .btn-primary {
  float: right;
  width: 200px;
  text-align: center;
  font-size: 18px;
  margin-left: 50px;
  background: #ff6700;
  color: #fff;
}
.shoppingCart .cart-bar .cart-bar-right .btn-primary-disabled {
  float: right;
  width: 200px;
  text-align: center;
  font-size: 18px;
  margin-left: 50px;
  background: #e0e0e0;
  color: #b0b0b0;
}
.shoppingCart .cart-bar .cart-bar-right .btn-primary:hover {
  background-color: #f25807;
}
/* 璐墿杞﹀簳閮ㄥ鑸潯CSS END */
/* 璐墿杞︿富瑕佸唴瀹瑰尯CSS END */

/* 璐墿杞︿负绌虹殑鏃跺€欐樉绀虹殑鍐呭CSS */
.shoppingCart .cart-empty {
  width: 1225px;
  margin: 0 auto;
}
.shoppingCart .cart-empty .empty {
  height: 300px;
  padding: 0 0 130px 558px;
  margin: 65px 0 0;
  background: url(../assets/image/cart-empty.png) no-repeat 124px 0;
  color: #b0b0b0;
  overflow: hidden;
}
.shoppingCart .cart-empty .empty h2 {
  margin: 70px 0 15px;
  font-size: 36px;
}
.shoppingCart .cart-empty .empty p {
  margin: 0 0 20px;
  font-size: 20px;
}
/* 璐墿杞︿负绌虹殑鏃跺€欐樉绀虹殑鍐呭CSS END */
</style>

