<template>
  <div class="goods" id="goods" name="goods">
    <div class="breadcrumb">
      <el-breadcrumb separator-class="el-icon-arrow-right">
        <el-breadcrumb-item to="/">首页</el-breadcrumb-item>
        <el-breadcrumb-item>全部商品</el-breadcrumb-item>
        <el-breadcrumb-item v-if="brandName">品牌</el-breadcrumb-item>
        <el-breadcrumb-item v-else-if="search">搜索</el-breadcrumb-item>
        <el-breadcrumb-item v-else>分类</el-breadcrumb-item>
        <el-breadcrumb-item v-if="brandName">{{ brandName }}</el-breadcrumb-item>
        <el-breadcrumb-item v-else-if="search">{{ search }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="nav">
      <div class="product-nav">
        <div class="title">分类</div>
        <el-tabs v-model="activeName" type="card">
          <el-tab-pane
            v-for="(item,index) in category"
            :key="index"
            :label="item.categoryName"
            :name="item.categoryName"/>
        </el-tabs>
      </div>
    </div>

    <div class="main">
      <div class="list">
        <MyList :list="products" v-if="products.length > 0"></MyList>
        <EsList :list="esproducts" v-else-if="esproducts.length > 0"></EsList>
        <div v-else class="none-product">抱歉没有找到相关的商品，请看看其他商品</div>
      </div>
      <div class="pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :page-size="pageSize"
          :total="total"
          :current-page="currentPage"
          @current-change="currentChange"
        ></el-pagination>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      category: [],
      categoryName: "",
      products: [],
      esproducts: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      activeName: "全部商品",
      search: "",
      brandId: null,
      brandName: "",
      changingRoute: false
    };
  },
  created() {
    this.syncRouteQuery();
    this.getProducts();
  },
  watch: {
    activeName(val) {
      if (this.changingRoute || !val) {
        return;
      }
      if (val === "全部商品") {
        this.brandId = null;
        this.brandName = "";
        this.search = "";
        this.categoryName = "";
        this.currentPage = 1;
        this.$router.push({ path: "/goods" });
        this.getProducts();
        return;
      }
      this.brandId = null;
      this.brandName = "";
      this.search = "";
      this.categoryName = val;
      this.currentPage = 1;
      this.$router.push({
        path: "/goods",
        query: { categoryName: val }
      });
      this.getProducts();
    },
    $route(val) {
      if (val.path !== "/goods") {
        return;
      }
      this.currentPage = 1;
      this.syncRouteQuery();
      this.getProducts();
    }
  },
  methods: {
    syncRouteQuery() {
      const query = this.$route.query || {};
      this.changingRoute = true;
      this.brandId = query.brandId ? Number(query.brandId) : null;
      this.brandName = query.brandName || "";
      this.search = query.search || query.categoryName || "";
      this.categoryName = query.categoryName || "";
      this.activeName = this.categoryName || "全部商品";
      this.changingRoute = false;
    },
    backtop() {
      const timer = setInterval(function() {
        const top = document.documentElement.scrollTop || document.body.scrollTop;
        const speed = Math.floor(-top / 5);
        document.documentElement.scrollTop = document.body.scrollTop = top + speed;
        if (top === 0) {
          clearInterval(timer);
        }
      }, 20);
    },
    currentChange(currentPage) {
      this.currentPage = currentPage;
      this.getProducts();
      this.backtop();
    },
    getProducts() {
      const url = this.brandId ? "/item/skubrandlist" : "/item/skucidlist";
      const params = {
        page: this.currentPage,
        rows: this.pageSize
      };
      if (this.brandId) {
        params.brandId = this.brandId;
      } else if (this.categoryName) {
        params.categoryName = this.categoryName;
      } else if (this.search) {
        params.key = this.search;
      }
      this.$http.get(url, { params }).then(resp => {
        this.setGoodsData(resp.data || {});
      }).catch(() => {
        this.products = [];
        this.esproducts = [];
        this.total = 0;
      });
    },
    setGoodsData(goods) {
      const names = Array.isArray(goods.cname) ? goods.cname.slice() : [];
      if (!names.includes("全部商品")) {
        names.unshift("全部商品");
      }
      this.category = names.map(name => ({ categoryName: name }));
      this.products = goods.skus && Array.isArray(goods.skus.items) ? goods.skus.items : [];
      this.total = goods.skus && goods.skus.total ? goods.skus.total : 0;
      this.esproducts = [];
    },
    getProductBySearch() {
      this.$http.post("/search/page", {
        key: this.search,
        page: this.currentPage,
        size: this.pageSize
      }).then(({ data }) => {
        const items = Array.isArray(data.items) ? data.items : [];
        items.forEach(item => {
          item.skus = JSON.parse(item.skus);
          item.selected = item.skus[0];
        });
        this.esproducts = items;
        this.products = [];
        this.total = data.total || 0;
      }).catch(() => {
        this.esproducts = [];
        this.products = [];
        this.total = 0;
      });
    }
  }
}
</script>

<style scoped>
.goods {
  background-color: #f5f5f5;
}
.el-tabs--card .el-tabs__header {
  border-bottom: none;
}
.goods .breadcrumb {
  height: 50px;
  background-color: white;
}
.goods .breadcrumb .el-breadcrumb {
  width: 1225px;
  line-height: 30px;
  font-size: 16px;
  margin: 0 auto;
}
.goods .nav {
  background-color: white;
}
.goods .nav .product-nav {
  width: 1225px;
  height: 40px;
  line-height: 40px;
  margin: 0 auto;
}
.nav .product-nav .title {
  width: 50px;
  font-size: 16px;
  font-weight: 700;
  float: left;
}
.goods .main {
  margin: 0 auto;
  max-width: 1225px;
}
.goods .main .list {
  min-height: 650px;
  padding-top: 14.5px;
  margin-left: -13.7px;
  overflow: auto;
}
.goods .main .pagination {
  height: 50px;
  text-align: center;
}
.goods .main .none-product {
  color: #333;
  margin-left: 13.7px;
}
</style>
