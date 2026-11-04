// Yükleniyor ve hata gibi tek cümlelik ekranlar.
export function MessageScreen({ message }: { message: string }) {
  return (
    <div className="app">
      <div className="screen">
        <p className="lede">{message}</p>
      </div>
    </div>
  );
}
