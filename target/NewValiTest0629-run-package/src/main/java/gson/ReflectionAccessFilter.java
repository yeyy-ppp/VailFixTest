package gson;
import gson.internal.ReflectionAccessFilterHelper;
public interface ReflectionAccessFilter {
    enum FilterResult {
    ALLOW,
    INDECISIVE,
    BLOCK_INACCESSIBLE,
    BLOCK_ALL
  }
  ReflectionAccessFilter BLOCK_INACCESSIBLE_JAVA =
      new ReflectionAccessFilter() {
        @Override
        public FilterResult check(Class<?> rawClass) {
          return ReflectionAccessFilterHelper.isJavaType(rawClass)
              ? FilterResult.BLOCK_INACCESSIBLE
              : FilterResult.INDECISIVE;
        }
        @Override
        public String toString() {
          return "ReflectionAccessFilter#BLOCK_INACCESSIBLE_JAVA";
        }
      };
  ReflectionAccessFilter BLOCK_ALL_JAVA =
      new ReflectionAccessFilter() {
        @Override
        public FilterResult check(Class<?> rawClass) {
          return ReflectionAccessFilterHelper.isJavaType(rawClass)
              ? FilterResult.BLOCK_ALL
              : FilterResult.INDECISIVE;
        }
        @Override
        public String toString() {
          return "ReflectionAccessFilter#BLOCK_ALL_JAVA";
        }
      };
  ReflectionAccessFilter BLOCK_ALL_ANDROID =
      new ReflectionAccessFilter() {
        @Override
        public FilterResult check(Class<?> rawClass) {
          return ReflectionAccessFilterHelper.isAndroidType(rawClass)
              ? FilterResult.BLOCK_ALL
              : FilterResult.INDECISIVE;
        }
        @Override
        public String toString() {
          return "ReflectionAccessFilter#BLOCK_ALL_ANDROID";
        }
      };
  ReflectionAccessFilter BLOCK_ALL_PLATFORM =
      new ReflectionAccessFilter() {
        @Override
        public FilterResult check(Class<?> rawClass) {
          return ReflectionAccessFilterHelper.isAnyPlatformType(rawClass)
              ? FilterResult.BLOCK_ALL
              : FilterResult.INDECISIVE;
        }
        @Override
        public String toString() {
          return "ReflectionAccessFilter#BLOCK_ALL_PLATFORM";
        }
      };
  FilterResult check(Class<?> rawClass);
}