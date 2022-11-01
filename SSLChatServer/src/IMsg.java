public interface IMsg {
  boolean Dispatch(Object paramObject, String paramString);
  
  boolean Login(Object paramObject);
  
  boolean Logout(Object paramObject);
}
