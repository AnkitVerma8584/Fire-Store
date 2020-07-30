package dataStorageClasses;

public class Upload {
    String link , name ;

    public Upload() {
    }

    public Upload(String link, String name) {
        this.link = link;
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public String getName() {
        return name;
    }
}
