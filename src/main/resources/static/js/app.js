document.getElementById("searchBtn")
    .addEventListener("click", searchMusic);

document.getElementById("keyword")
    .addEventListener("keydown", function(event) {

        if (event.key === "Enter") {
            searchMusic();
        }

    });

async function searchMusic() {

    const keyword =
        document.getElementById("keyword").value;

    const response =
        await fetch(`/api/music/search?keyword=${keyword}`);

    const musicList =
        await response.json();

    renderMusicList(musicList);
}

function renderMusicList(musicList) {

    const resultArea =
        document.getElementById("resultArea");

    resultArea.innerHTML = "";

    musicList.forEach(music => {

        resultArea.innerHTML += `
            <div>
                <h3>${music.title}</h3>
                <p>가수 : ${music.artist}</p>
                <p>앨범 : ${music.album}</p>
                <p>장르 : ${music.genre}</p>
                <hr>
            </div>
        `;
    });
}