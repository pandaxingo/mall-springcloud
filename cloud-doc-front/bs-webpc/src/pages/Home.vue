<template>
  <div class="home" id="home" name="home" @click="closeCategoryMenu">
    <div class="block">
      <div class="menu" @click.stop>
        <ul class="menu-wrap" v-for="cid1 in categoryOnes" :key="cid1.id">
          <li
            class="menu-item"
            :class="{ active: activeLevelOneId == cid1.id, 'menu-expanded': menuExpanded && activeLevelOneId == cid1.id }"
          >
            <a href="javascript:;" @click.stop="selectCategory(cid1, 1)">{{ cid1.name }}</a>
            <div class="two">
              <ul
                v-for="item in categoryTwos"
                :key="item.id"
                :class="{ active: activeLevelTwoId == item.id }"
              >
                <a href="javascript:;" @click.stop="selectCategory(item, 2)">{{ item.name }}</a>
                <div class="three" :class="{ active: activeLevelTwoId == item.id }">
                  <li v-for="item2 in categoryThrees" :key="item2.id">
                    <a
                      href="javascript:;"
                      :class="{ active: activeLevelThreeId == item2.id }"
                      @click.stop="selectCategory(item2, 3)"
                    >{{ item2 ? item2.name : "" }}</a>
                  </li>
                </div>
              </ul>
            </div>
          </li>
        </ul>
      </div>

      <el-carousel height="460px">
        <el-carousel-item v-for="item in carousel" :key="item.id">
          <img style="height:460px;" :src="item.img" :alt="item.id" />
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="main-box">
      <div class="main">
        <div class="brand-section">
          <div class="box-hd">
            <div class="title">{{ activeCategoryName }}品牌</div>
          </div>
          <div class="brand-list" v-if="brands.length > 0">
            <div class="brand-card" v-for="brand in brands" :key="brand.id" @click="goBrandGoods(brand)">
              <img :src="$imageUrl(brand.image)" @error="$imageError" />
              <span>{{ brand.name }}</span>
            </div>
          </div>
          <div class="brand-empty" v-else>当前分类暂无品牌</div>
        </div>

        <div class="veget">
          <div class="box-hd">
            <div class="title">畅销蔬菜</div>
          </div>
          <div class="box-bd">
            <div class="promo-list">
              <router-link to>
                <img src="/imgs/ads/home-vegetables.png" class="veget-img" />
              </router-link>
            </div>
            <div class="list">
              <MyList2 :list="vegetables" :isMore="true"></MyList2>
            </div>
          </div>
        </div>

        <div class="hr-line"></div>

        <div class="fruit">
          <div class="box-hd">
            <div class="title">新鲜水果</div>
          </div>
          <div class="box-bd">
            <div class="promo-list">
              <router-link to>
                <img src="/imgs/ads/home-fruits.png" class="fruit-img"/>
              </router-link>
            </div>
            <div class="list">
              <MyList2 :list="fruitList" :isMore="false"></MyList2>
            </div>
          </div>
        </div>

        <div class="mechine-3">
          <div class="mechine">
            <div class="box-hd">
              <div class="title">热销农机</div>
            </div>
            <div class="box-bd">
              <div class="promo-list">
                <router-link to>
                  <img src="/imgs/ads/home-machinery.png" class="mechine-img" />
                </router-link>
              </div>
              <div class="list">
                <MyList2 :list="agrimachinery" :isMore="true"></MyList2>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  name: "Home",
  data() {
    return {
      carousel: [
        { id: "42", img: "/imgs/slider/slide-1.jpg" },
        { id: "52", img: "/imgs/slider/slide-2.jpg" },
        { id: "53", img: "/imgs/slider/slide-3.jpg" },
        { id: "54", img: "/imgs/slider/slide-4.jpg" },
        { id: "55", img: "/imgs/slider/slide-5.jpg" }
      ],
      categoryOnes: [],
      categoryTwos: [],
      categoryThrees: [],
      brands: [],
      activeCategoryId: null,
      activeCategoryName: "分类",
      activeLevelOneId: null,
      activeLevelTwoId: null,
      activeLevelThreeId: null,
      menuExpanded: false,
      fruitList: [],
      vegetables: [],
      agrimachinery: []
    };
  },
  created() {
    this.getCategoryOneList();
    this.getfruitList();
    this.getFreshFruitList();
    this.getagrimachinery();
  },
  methods: {
    selectCategory(category, level) {
      if (!category || !category.id) {
        return;
      }
      this.activeCategoryId = category.id;
      this.activeCategoryName = category.name || "分类";
      this.menuExpanded = true;
      this.getBrandsByCid(category.id);
      if (level === 1) {
        this.activeLevelOneId = category.id;
        this.activeLevelTwoId = null;
        this.activeLevelThreeId = null;
        this.getCategoryTwoList(category.id);
        this.categoryThrees = [];
      }
      if (level === 2) {
        this.activeLevelTwoId = category.id;
        this.activeLevelThreeId = null;
        this.getCategoryThreeList(category.id);
      }
      if (level === 3) {
        this.activeLevelThreeId = category.id;
      }
    },
    closeCategoryMenu() {
      this.menuExpanded = false;
    },
    getBrandsByCid(cid) {
      this.$http
        .get("/item/brand/cid/" + cid)
        .then(resp => {
          this.brands = Array.isArray(resp.data) ? resp.data : [];
        })
        .catch(error => {
          if (error.response && error.response.status === 404) {
            this.brands = [];
            return;
          }
          console.log("品牌加载失败：" + error);
        });
    },
    goBrandGoods(brand) {
      if (!brand || !brand.id) {
        return;
      }
      this.$router.push({
        path: "/goods",
        query: {
          brandId: brand.id,
          brandName: brand.name
        }
      });
    },
    getfruitList(){
      this.$http.get("/item/sku/home/vegetables").then(resp=>{
        const list = Array.isArray(resp.data) ? resp.data : [];
        if (list.length > 0) {
          this.vegetables = list;
          return;
        }
        return this.$http.get("/item/sku/list?id=18").then(oldResp => {
          this.vegetables = oldResp.data || [];
        });
      }).catch(resp=>{
        console.log("异常信息：" + resp);
      });
    },
    getFreshFruitList(){
      this.$http.get("/item/sku/home/fruits").then(resp => {
        const list = Array.isArray(resp.data) ? resp.data : [];
        if (list.length > 0) {
          this.fruitList = list.slice(0, 8);
          return;
        }
        return this.loadFreshFruitByCategory();
      }).catch(error => {
        console.log("新鲜水果加载失败：" + error);
        this.loadFreshFruitByCategory();
      });
    },
    loadFreshFruitByCategory(){
      return this.$http.get("/item/category/name", {
        params: {
          name: "新鲜水果"
        }
      }).then(resp => {
        const categories = resp.data || [];
        const category = categories.find(item => item.name === "新鲜水果") || categories[0];
        if (!category || !category.id) {
          this.fruitList = [];
          return;
        }
        return this.$http.get("/item/sku/cid/" + category.id);
      }).then(resp => {
        if (resp) {
          this.fruitList = Array.isArray(resp.data) ? resp.data.slice(0, 8) : [];
        }
      }).catch(error => {
        this.fruitList = [];
        console.log("新鲜水果加载失败：" + error);
      });
    },
    getagrimachinery(){
      this.$http.get("/item/sku/home/machinery").then(resp=>{
        const list = Array.isArray(resp.data) ? resp.data : [];
        if (list.length > 0) {
          this.agrimachinery = list;
          return;
        }
        return this.$http.get("/item/sku/list?id=13").then(oldResp => {
          this.agrimachinery = oldResp.data || [];
        });
      }).catch(resp=>{
        console.log("异常信息：" + resp);
      });
    },
    getCategoryOneList(){
      this.$http.get("/item/category/list",{
        params:{ pid:0 }
      }).then(resp=>{
        this.categoryOnes = resp.data || [];
        if (this.categoryOnes.length > 0) {
          this.selectCategory(this.categoryOnes[0], 1);
          this.menuExpanded = false;
        }
      }).catch(resp=>{
        console.log("异常信息：" + resp);
      });
    },
    getCategoryTwoList(cid2){
      this.$http.get("/item/category/list",{
        params:{ pid:cid2 }
      }).then(resp=>{
        this.categoryTwos = resp.data || [];
      }).catch(resp=>{
        console.log("异常信息：" + resp);
      });
    },
    getCategoryThreeList(cid3){
      this.$http.get("/item/category/list",{
        params:{ pid:cid3 }
      }).then(resp=>{
        this.categoryThrees = resp.data || [];
      }).catch(resp=>{
        console.log("异常信息：" + resp);
      });
    }
  }
};
</script>
<style scoped lang="scss">
@import "../assets/style/index.css";
.brand-section {
  margin: 20px 0 30px;
  background: #fff;
}
.brand-list {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
  padding: 0 0 10px;
}
.brand-card {
  height: 112px;
  border: 1px solid #e0e0e0;
  background: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #424242;
  cursor: pointer;
  transition: border-color .2s, box-shadow .2s;
}
.brand-card:hover {
  border-color: #ff6700;
  box-shadow: 0 4px 14px rgba(0, 0, 0, .08);
}
.brand-card img {
  width: 86px;
  height: 54px;
  object-fit: contain;
  margin-bottom: 10px;
}
.brand-card span {
  width: 100%;
  padding: 0 8px;
  box-sizing: border-box;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.brand-empty {
  height: 90px;
  line-height: 90px;
  color: #999;
  text-align: center;
  border: 1px dashed #ddd;
  background: #fafafa;
}
.menu {
  background-color: #55555a7a;
  box-sizing: border-box;
  position: absolute;
  width: 264px;
  height: 451px;
  z-index: 8;
  padding: 26px 0;

  .menu-wrap {
    .menu-item {
      height: 50px;
      line-height: 50px;
      a {
        position: relative;
        font-size: 16px;
        height: 42px;
        line-height: 42px;
        display: block;
        color: white;
        padding-left: 30px;

        &:after {
          position: absolute;
          top: 0;
          right: 70px;
          content: "\e6ef";
          color: #e0e0e0;
          font-family: iconfont;
        }
      }
      &:hover,
      &.active {
        background-color: #55555a7a;
      }
      &:hover {
        .two {
          display: block;
        }
        .three {
          display: block;
        }
      }
      &.menu-expanded {
        background-color: rgba(255, 103, 0, .78);
        .two {
          display: block;
        }
      }
      &.active > a,
      &.menu-expanded > a {
        color: #fff;
        font-weight: 700;
      }
    }
    // 浜岀骇绫荤洰
    .two {
      background-color: #55555a7a;
      display: none;
      width: 650px;
      height: 451px;
      position: absolute;
      border: 1px gray;
      top: 0;
      left: 263px;
      ul {
        display: flex;
        justify-content: space-between;
        height: 75px;
        padding-left: 23px;
        &.active > a {
          color: #fff;
          font-weight: 700;
          background-color: rgba(255, 103, 0, .72);
        }
        // 涓夌骇绫荤洰
        .three {
          width: 500px;
          height: 451px;
          position: absolute;
          border: 1px gray;
          top: 0;
          left: 300px;
          display: none;
          &.active {
            display: block;
          }
        }
        li {
          height: 75px;
          width: 100px;
          line-height: 75px;
          flex: 1;
          padding-left: 23px;
        }
        a {
          color: whitesmoke;
          font-size: 14px;
          &.active {
            color: #fff;
            font-weight: 700;
            background-color: rgba(255, 103, 0, .72);
          }
        }
      }
    }
  }
  
}
  // 姘存灉CSS鏍峰紡
  .veget,
  .fruit,
  .mechine,
  .box-bd,
  .box-bd-1,
  .mechine-3 {
    overflow: hidden;
  }
  .fruit {
    margin-bottom: 30px;
  }
  .mechine{
      clear: both;
      float: none;
      padding-right: 0;
      margin-top: 10px;
      background-color: #f1f3f7;
  }
  .mechine .box-bd {
      min-height: 650px;
  }
  .mechine .list {
      min-height: 650px;
      background-color: #f1f3f7;
  }
 .fruit-img{
      width: 227px;
      height: 320px;
      object-fit: cover;
  }
  .mechine-img{
      width: 234px;
      height: 650px;
      object-fit: cover;
  }
  .veget-img{
      width: 234px;
      height: 650px;
      object-fit: cover;
  }
  .hr-line{
      margin-top: 30px;
      height: 20px;
  }
  // 璁剧疆鐑攢鏈烘鐨勮儗鏅壊
  .mechine-3{
    // border: 1px red solid;
    width: 100%;
    clear: both;
    float: none;
    margin-left: 0;
    padding-left: 0;
    padding-bottom: 10px;
    background-color:  #f1f3f7;
  }

</style>

